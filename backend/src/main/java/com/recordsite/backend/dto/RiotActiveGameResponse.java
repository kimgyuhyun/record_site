package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Spectator-V5 CurrentGameInfo 매핑.
// 게임 시작 시점에 확정된 정보(챔피언/스펠/룬/팀/밴)만 담긴다 — 실시간 체력·골드 등은 없음.
// Spectator-V5부터 참가자 식별이 summonerName/summonerId → puuid + riotId 로 바뀌었다.
@Getter
@Setter
@NoArgsConstructor
public class RiotActiveGameResponse {

    private Long gameId;
    private String gameType;
    private String gameMode;
    private Long gameStartTime;      // epoch ms (로딩 중이면 0)
    private Long gameLength;         // 현재까지 진행 시간(초)
    private Long mapId;
    private Long gameQueueConfigId;  // 큐 ID (420 솔랭 등)
    private String platformId;

    private List<BannedChampion> bannedChampions;
    private List<CurrentGameParticipant> participants;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BannedChampion {
        private Integer championId;
        private Long teamId;
        private Integer pickTurn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CurrentGameParticipant {
        private String puuid;
        private Long teamId;
        private Integer spell1Id;
        private Integer spell2Id;
        private Integer championId;
        private Long profileIconId;
        private String riotId;  // "게임이름#태그" 형식
        private boolean bot;
        private Perks perks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Perks {
        private List<Long> perkIds;  // perkIds[0] = 핵심룬(키스톤)
        private Long perkStyle;       // 주 룬 계열
        private Long perkSubStyle;    // 보조 룬 계열
    }
}
