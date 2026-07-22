package com.recordsite.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    // 모든 Riot API 호출이 레이트리밋 인터셉터를 거치도록 RestTemplate 에 등록한다.
    // (ddragon CDN 등 Riot API 외 호스트는 인터셉터 내부에서 throttle 없이 통과)
    @Bean
    public RestTemplate restTemplate(RiotApiRateLimiter riotApiRateLimiter,
                                     io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        // 타임아웃이 없으면 응답이 안 오는 Riot 호출 하나가 단일 갱신 워커를 무한정 붙잡아
        // 이후 모든 갱신이 "갱신중"에서 멈춘다. 레이트리밋 대기(acquire)는 호출 전 단계라
        // 이 소켓 타임아웃에 걸리지 않으므로, 스로틀은 그대로 두고 네트워크 행만 끊는다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new RiotApiClientInterceptor(riotApiRateLimiter, meterRegistry));
        return restTemplate;
    }
    // RestTemplate는 Java에서 HTTP 요청을 보내는 도구
    // 브라우저에서 URL 치고 들어가는 걸 코드로 하는것

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }

    // 전적 갱신 시 신규 매치들을 동시에 수집하기 위한 고정 크기 풀.
    // 동시 개수에 상한을 둬서 (1) Riot 공유 예산을 한 번에 다 쓰지 않고, (2) DB 커넥션 풀(HikariCP) 한도를
    // 넘지 않게 한다. 실제 호출 속도는 RiotApiRateLimiter 가 최종적으로 제어한다.
    @Bean(destroyMethod = "shutdown")
    public ExecutorService matchCollectExecutor(
            @Value("${riot.refresh.collect-concurrency:5}") int concurrency) {
        return Executors.newFixedThreadPool(concurrency);
    }
}
