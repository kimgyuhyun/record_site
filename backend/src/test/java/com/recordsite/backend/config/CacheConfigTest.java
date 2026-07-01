package com.recordsite.backend.config;

import com.recordsite.backend.dto.PlayedChampionStatDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// 캐시 값 직렬화기가 List<record> 를 그대로 라운드트립하는지 검증.
// 기존 설정(As.PROPERTY)은 List 루트에 타입정보를 못 붙여 읽기에서 깨졌다 — 그 회귀를 막는다.
class CacheConfigTest {

    @Test
    @DisplayName("List<record> 캐시 값이 타입 유지한 채 직렬화/역직렬화된다")
    void listOfRecords_roundTrips() {
        GenericJackson2JsonRedisSerializer serializer = CacheConfig.redisValueSerializer();

        List<PlayedChampionStatDto> original = List.of(
                new PlayedChampionStatDto(777, "Yone", 2, 1, 1, 50, 2.83, 6.5, 3.0, 2.0, 249.0, 13567.0),
                new PlayedChampionStatDto(75, "Nasus", 1, 0, 1, 0, 0.83, 2.0, 6.0, 3.0, 115.0, 7253.0));

        byte[] bytes = serializer.serialize(original);
        Object restored = serializer.deserialize(bytes);

        List<?> list = assertInstanceOf(List.class, restored);
        assertEquals(2, list.size());
        assertInstanceOf(PlayedChampionStatDto.class, list.get(0)); // 원소가 Map 이 아니라 record 로 복원되어야 한다
        assertEquals(original, restored);                            // 값도 완전 동일
    }

    @Test
    @DisplayName("빈 리스트도 라운드트립된다")
    void emptyList_roundTrips() {
        GenericJackson2JsonRedisSerializer serializer = CacheConfig.redisValueSerializer();

        byte[] bytes = serializer.serialize(List.of());
        Object restored = serializer.deserialize(bytes);

        assertEquals(List.of(), restored);
    }
}
