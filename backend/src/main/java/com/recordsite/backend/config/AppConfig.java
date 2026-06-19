package com.recordsite.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // 모든 Riot API 호출이 레이트리밋 인터셉터를 거치도록 RestTemplate 에 등록한다.
    // (ddragon CDN 등 Riot API 외 호스트는 인터셉터 내부에서 throttle 없이 통과)
    @Bean
    public RestTemplate restTemplate(RiotApiRateLimiter riotApiRateLimiter) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new RiotApiClientInterceptor(riotApiRateLimiter));
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
}
