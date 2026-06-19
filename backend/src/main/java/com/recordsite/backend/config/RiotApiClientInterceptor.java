package com.recordsite.backend.config;

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
            ClientHttpResponse response = execution.execute(request, body);

            if (response.getStatusCode().value() != TOO_MANY_REQUESTS || attempt >= MAX_RETRY_ON_429) {
                return response;
            }

            long retryAfterMs = parseRetryAfterMs(response);
            log.warn("Riot 429 수신 → {}ms 후 재시도 ({}/{}): {}",
                    retryAfterMs, attempt + 1, MAX_RETRY_ON_429, request.getURI());
            response.close();
            backoff(retryAfterMs);
        }
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
