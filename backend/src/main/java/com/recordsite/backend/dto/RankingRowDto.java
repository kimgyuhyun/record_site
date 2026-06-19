package com.recordsite.backend.dto;

import com.recordsite.backend.entity.LadderEntry;

import java.util.List;

// 랭킹 페이지 한 행. 사다리 스냅샷(LadderEntry) + 우리 DB 기준 모스트 챔피언.
public record RankingRowDto(
        int rankPosition,
        String gameName,
        String tagLine,
        String tier,
        int leaguePoints,
        int wins,
        int losses,
        int winRate,                  // 0~100 (%)
        List<Integer> mostChampionIds // 우리 DB에 수집된 매치 기준 상위 픽(없으면 빈 목록)
) {

    public static RankingRowDto of(LadderEntry entry, List<Integer> mostChampionIds) {
        int games = entry.getWins() + entry.getLosses();
        int winRate = games == 0 ? 0 : Math.round(entry.getWins() * 100.0f / games);
        return new RankingRowDto(
                entry.getRankPosition(),
                entry.getGameName(),
                entry.getTagLine(),
                entry.getTier().name(),
                entry.getLeaguePoints(),
                entry.getWins(),
                entry.getLosses(),
                winRate,
                mostChampionIds
        );
    }
}
