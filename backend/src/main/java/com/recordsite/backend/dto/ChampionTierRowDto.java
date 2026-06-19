package com.recordsite.backend.dto;

// 챔피언 분석(티어 리스트) 한 행. 승률/픽률은 자체 수집한 매치 DB 집계 기반이다.
//  - score: 표본이 작을 때 승률이 50%로 수렴하도록 베이지안 보정한 값(작은 표본 왜곡 방지) + 픽률 소량 가중.
//  - tier: 전체를 score 내림차순 정렬한 뒤 백분위로 부여하므로, 단건 of() 시점엔 빈 값이고 서비스에서 채운다.
public record ChampionTierRowDto(
        int championId,
        String championName,
        String position,   // Riot teamPosition (TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY)
        String tier,        // OP / 1 / 2 / 3 / 4
        double score,
        double winRate,     // 0~100 (%)
        double pickRate,    // 0~100 (%)
        double banRate,     // 0~100 (%) — 밴된 매치 수 ÷ 전체 매치 수
        long games
) {

    // 작은 표본 보정 강도(가상의 50% 승부 PRIOR 판). 클수록 적은 표본이 더 강하게 50%로 수렴.
    private static final double SAMPLE_PRIOR = 20.0;

    public static ChampionTierRowDto of(int championId, String championName, String position,
                                        long games, long wins, long banCount, long totalMatches) {
        double winRate = games == 0 ? 0.0 : wins * 100.0 / games;
        double pickRate = totalMatches == 0 ? 0.0 : games * 100.0 / totalMatches;
        double banRate = totalMatches == 0 ? 0.0 : Math.min(banCount * 100.0 / totalMatches, 100.0);

        double adjustedWinRate = (wins + SAMPLE_PRIOR * 0.5) / (games + SAMPLE_PRIOR) * 100.0;
        double score = adjustedWinRate + Math.min(pickRate, 30.0) * 0.1;

        return new ChampionTierRowDto(
                championId, championName, position, "",
                round(score, 2), round(winRate, 1), round(pickRate, 1), round(banRate, 1), games);
    }

    public ChampionTierRowDto withTier(String tier) {
        return new ChampionTierRowDto(
                championId, championName, position, tier, score, winRate, pickRate, banRate, games);
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
