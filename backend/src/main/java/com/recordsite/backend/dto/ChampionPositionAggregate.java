package com.recordsite.backend.dto;

// 전역 챔피언 통계 집계의 원시 결과 (챔피언 × 포지션 단위, DB에서 바로 계산 가능한 값만).
// 챔피언 단위 합산/승률/픽률/티어는 ChampionStatService 에서 계산한다(쿼리/표현 분리).
public record ChampionPositionAggregate(
        Integer championId,
        String championName,
        String teamPosition,
        Long games,
        Long wins
) {
}
