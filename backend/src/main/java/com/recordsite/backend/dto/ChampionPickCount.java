package com.recordsite.backend.dto;

// 특정 puuid 가 특정 챔피언을 플레이한 횟수(랭킹 페이지의 '모스트 챔피언' 계산용 원시 집계).
public record ChampionPickCount(
        String puuid,
        Integer championId,
        Long games
) {
}
