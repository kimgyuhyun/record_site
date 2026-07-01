package com.recordsite.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 캐시 설정.
 *  - 캐시별 TTL 을 다르게 둔다(정적 데이터는 길게, 집계 결과는 짧게).
 *  - 값 직렬화는 JSON(GenericJackson2JsonRedisSerializer) — DTO/record 그대로 저장한다.
 *  - Redis 장애가 사용자 요청을 깨지 않도록 에러를 로깅만 하고 삼킨다(캐시 미스처럼 DB 로 폴백).
 *    → Redis 가 죽거나 로컬에 없어도 사이트는 (느려질 뿐) 정상 동작한다.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // 정적 데이터(패치 단위로만 바뀜) — 길게 캐싱
    public static final String STATIC_CHAMPIONS = "staticChampions";
    public static final String STATIC_ITEMS = "staticItems";
    public static final String STATIC_RUNE_PATHS = "staticRunePaths";
    public static final String STATIC_RUNES = "staticRunes";
    // 무거운 집계 — 짧게 캐싱(데이터가 점진적으로 쌓이므로 일정 시간 신선도 희생)
    public static final String CHAMPION_TIER_LIST = "championTierList";
    public static final String PLAYED_CHAMPIONS = "playedChampions";

    private static final Duration STATIC_TTL = Duration.ofHours(12);
    private static final Duration TIER_LIST_TTL = Duration.ofMinutes(10);
    private static final Duration PLAYED_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisValueSerializer()));

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                STATIC_CHAMPIONS, base.entryTtl(STATIC_TTL),
                STATIC_ITEMS, base.entryTtl(STATIC_TTL),
                STATIC_RUNE_PATHS, base.entryTtl(STATIC_TTL),
                STATIC_RUNES, base.entryTtl(STATIC_TTL),
                CHAMPION_TIER_LIST, base.entryTtl(TIER_LIST_TTL),
                PLAYED_CHAMPIONS, base.entryTtl(PLAYED_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(TIER_LIST_TTL))
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    // 캐시 값 직렬화기. 기본 GenericJackson2JsonRedisSerializer(타입정보 As.PROPERTY)는 List 를 루트로 저장할 때
    // JSON 배열에 @class 속성을 붙일 수 없어 타입 래퍼 없이 "[{...},{...}]" 로 쓰는데, 읽을 때는 배열 맨 앞에서
    // 타입ID(문자열)를 기대하다 깨진다 → List<DTO> 를 반환하는 캐시(playedChampions/championTierList/static)가
    // 전부 역직렬화 실패했다. 타입정보를 WRAPPER_ARRAY(["type", value])로 넣으면 List 루트와 원소가 함께 감싸져
    // 라운드트립된다. final record 도 타입정보가 필요하므로 EVERYTHING 으로 모든 비원시 값에 타입을 남긴다.
    static GenericJackson2JsonRedisSerializer redisValueSerializer() {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class) // 우리가 직접 넣고 빼는 내부 캐시라 모든 타입 허용
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                typeValidator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.WRAPPER_ARRAY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // Redis 가 불통이어도 요청은 살린다: 캐시 연산 실패를 로깅만 하고 무시한다.
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("캐시 조회 실패(무시하고 DB 폴백): cache={}, key={}, error={}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("캐시 적재 실패(무시): cache={}, key={}, error={}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("캐시 무효화 실패(무시): cache={}, key={}, error={}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("캐시 클리어 실패(무시): cache={}, error={}", cache.getName(), ex.getMessage());
            }
        };
    }
}
