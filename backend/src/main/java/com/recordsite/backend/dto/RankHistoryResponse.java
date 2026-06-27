package com.recordsite.backend.dto;

import java.util.List;

// 티어/LP 변동 이력 — 큐별 스냅샷 시계열. 프론트가 LP 그래프(사다리 점수 추이)로 그린다.
public record RankHistoryResponse(
        List<Point> solo,
        List<Point> flex
) {
    // at: 스냅샷 시각(epoch millis). ladderScore: 티어·디비전·LP를 합친 절대 점수(그래프 Y축).
    public record Point(
            long at,
            String tier,
            String division,
            int leaguePoints,
            int ladderScore
    ) {}
}
