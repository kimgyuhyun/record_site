package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Riot Champion-Mastery-V4 응답 (챔피언 1개 분량)
@Getter
@Setter
@NoArgsConstructor
public class RiotChampionMasteryResponse {
    private int championId;
    private int championLevel;   // 숙련도 레벨
    private long championPoints;  // 숙련도 점수
    private long lastPlayTime;    // 마지막 플레이(epoch ms)
}
