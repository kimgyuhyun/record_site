package com.recordsite.backend.dto;

// 소환사가 플레이한 챔피언별 통계 응답 DTO (전적 카드 위 챔피언 통계 표).
// QueryDSL 원시 집계(PlayedChampionAggregate)에서 승률/KDA/패배 수를 계산해 만든다.
public record PlayedChampionStatDto(
        int championId,
        String championName,
        long games,
        long wins,
        long losses,
        int winRate,        // 0~100 (%)
        double kda,         // (K+A)/D, 데스 0이면 K+A
        double avgKills,
        double avgDeaths,
        double avgAssists,
        double avgCs,
        double avgGold
) {

    public static PlayedChampionStatDto from(PlayedChampionAggregate aggregate) {
        long games = aggregate.games();
        long wins = aggregate.wins();
        long losses = games - wins;
        int winRate = games == 0 ? 0 : (int) Math.round(wins * 100.0 / games);

        double avgDeaths = aggregate.avgDeaths();
        double kda = avgDeaths == 0.0
                ? aggregate.avgKills() + aggregate.avgAssists()
                : (aggregate.avgKills() + aggregate.avgAssists()) / avgDeaths;

        return new PlayedChampionStatDto(
                aggregate.championId(),
                aggregate.championName(),
                games,
                wins,
                losses,
                winRate,
                round(kda, 2),
                round(aggregate.avgKills(), 1),
                round(avgDeaths, 1),
                round(aggregate.avgAssists(), 1),
                round(aggregate.avgCs(), 1),
                round(aggregate.avgGold(), 0)
        );
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
