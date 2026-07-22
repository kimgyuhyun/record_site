package com.recordsite.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 모든 Riot API 호출에 레이트리밋을 적용하고 429(Too Many Requests)를 백오프 재시도한다.
 *
 * Riot API 호스트(*.api.riotgames.com)만 throttle 대상이며, ddragon CDN 등은 그대로 통과시킨다.
 * 429 응답은 Retry-After 헤더(초)를 존중해 대기 후 재시도하고, 한도 초과 시 응답을 그대로 돌려준다.
 */
@Slf4j
@RequiredArgsConstructor
public class RiotApiClientInterceptor implements ClientHttpRequestInterceptor {

    private static final String RIOT_API_HOST_SUFFIX = ".api.riotgames.com";
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int MAX_RETRY_ON_429 = 3;
    private static final long DEFAULT_RETRY_AFTER_MS = 1_000L;

    private final RiotApiRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String host = request.getURI().getHost();
        boolean isRiotApiCall = host != null && host.endsWith(RIOT_API_HOST_SUFFIX);
        if (!isRiotApiCall) {
            return execution.execute(request, body);
        }

        for (int attempt = 0; ; attempt++) {
            rateLimiter.acquire();
            // 실제 네트워크 호출 구간만 계측한다(레이트리밋 대기는 제외) — Riot 측 지연을 순수하게 본다.
            // outcome 라벨은 상태코드 계열(2xx/4xx/5xx/429)로 카디널리티를 묶어 시계열 폭증을 막는다.
            Timer.Sample sample = Timer.start(meterRegistry);
            ClientHttpResponse response = execution.execute(request, body);
            int status = response.getStatusCode().value();
            sample.stop(meterRegistry.timer("riot.api.requests",
                    "method", request.getMethod().name(), "outcome", outcome(status)));

            if (status == TOO_MANY_REQUESTS) {
                // 레이트리밋이 뚫린 횟수 — 이 값이 0 에 가깝게 유지되는 것이 리미터가 제 역할을 한다는 증거다.
                meterRegistry.counter("riot.api.rate_limited").increment();
            }
            if (status != TOO_MANY_REQUESTS || attempt >= MAX_RETRY_ON_429) {
                return response;
            }

            long retryAfterMs = parseRetryAfterMs(response);
            log.warn("Riot 429 수신 → {}ms 후 재시도 ({}/{}): {}",
                    retryAfterMs, attempt + 1, MAX_RETRY_ON_429, request.getURI());
            response.close();
            backoff(retryAfterMs);
        }
    }

    // 상태코드를 계열 라벨로 압축한다(개별 코드/URI 를 라벨로 쓰면 시계열이 폭증하므로).
    private String outcome(int status) {
        if (status == TOO_MANY_REQUESTS) {
            return "429";
        }
        return (status / 100) + "xx";
    }

    // Retry-After 헤더(초 단위)를 ms 로 변환. 없거나 파싱 불가 시 기본 1초.
    private long parseRetryAfterMs(ClientHttpResponse response) throws IOException {
        String retryAfter = response.getHeaders().getFirst("Retry-After");
        if (retryAfter == null) {
            return DEFAULT_RETRY_AFTER_MS;
        }
        try {
            return Math.max(DEFAULT_RETRY_AFTER_MS, Long.parseLong(retryAfter.trim()) * 1_000L);
        } catch (NumberFormatException e) {
            return DEFAULT_RETRY_AFTER_MS;
        }
    }

    private void backoff(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Riot 429 백오프 대기 중 인터럽트", e);
        }
    }
}
