package com.recordsite.backend.dto;

// QueryDSL 집계 전용 원시 결과 (DB에서 바로 계산 가능한 값만).
// 승률/KDA/패배 수 같은 파생값은 PlayedChampionStatDto.from()에서 계산한다(쿼리/표현 분리).
public record PlayedChampionAggregate(
        Integer championId,
        String championName,
        Long games,
        Long wins,
        Double avgKills,
        Double avgDeaths,
        Double avgAssists,
        Double avgCs,
        Double avgGold
) {
}
