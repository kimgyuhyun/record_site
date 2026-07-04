package com.recordsite.backend.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonTimeConfig {

    // 이 앱의 모든 LocalDateTime 은 UTC 벽시계 시각이다(BackendApplication 이 JVM 기본 타임존을 UTC 로 고정).
    // 오프셋 없이 내려보내면 브라우저의 new Date() 가 이를 로컬 시각으로 오해해 실제와 어긋난다(KST 에서 9시간 과거로 표시되는 문제).
    // 따라서 응답 JSON 은 항상 'Z'(UTC) 를 붙여 내려, 클라이언트가 각자 로컬 시각으로 정확히 변환하게 한다.
    private static final DateTimeFormatter UTC_ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeAsUtc() {
        return builder -> builder.serializers(new LocalDateTimeSerializer(UTC_ISO));
    }
}
