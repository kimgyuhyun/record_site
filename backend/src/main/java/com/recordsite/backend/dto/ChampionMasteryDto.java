package com.recordsite.backend.dto;

// 챔피언 숙련도 응답 DTO (프론트 챔피언 숙련도 섹션용)
public record ChampionMasteryDto(
        int championId,
        int championLevel,
        long championPoints,
        long lastPlayTime
) {
    public static ChampionMasteryDto from(RiotChampionMasteryResponse res) {
        return new ChampionMasteryDto(
                res.getChampionId(),
                res.getChampionLevel(),
                res.getChampionPoints(),
                res.getLastPlayTime()
        );
    }
}
