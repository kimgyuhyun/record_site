package com.recordsite.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return  new RestTemplate();
    }
    // RestTemplate는 Java에서 HTTP 요청을 보내는 도구
    // 브라우저에서 URL 치고 들어가는 걸 코드로 하는것
}
