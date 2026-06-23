package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RiotParticipantResponse {

    private Integer participantId;
    private String puuid;
    private String riotIdGameName;
    private String riotIdTagline;
    private int teamId;
    private boolean win;
    private int championId;
    private String championName;
    private int kills;
    private int deaths;
    private int assists;

    private String teamPosition;
    private String individualPosition;

    private int item0; // 아이템
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;

    private int summoner1Id; // 스펠
    private int summoner2Id;

    private int goldEarned;

    private Long totalDamageDealt;
    private Long totalDamageDealtToChampions;
    private Long totalDamageTaken;

    private int visionScore;
    private int champLevel;


    private Perks perks;

    private boolean gameEndedInEarlySurrender; // 다시하기 여부
    private boolean teamEarlySurrendered;
    // 본인이 속한 팀이 다시하기를 당한 쪽인지 여부 탈주자 발생 팀 = true, win=false
    // 상대 팀 false, win=true

    private int totalMinionsKilled;
    private int neutralMinionsKilled;

    // ── 멀티킬/연속킬 ──
    private int doubleKills;
    private int tripleKills;
    private int quadraKills;
    private int pentaKills;
    private int largestMultiKill;
    private int largestKillingSpree;

    // ── 퍼스트 (개인) ──
    private boolean firstBloodKill;
    private boolean firstTowerKill;

    // ── 시야/와드 (visionScore 외 상세) ──
    private int wardsPlaced;
    private int wardsKilled;
    private int visionWardsBoughtInGame; // 제어 와드(핑크) 구매 수
    private int detectorWardsPlaced;

    // ── 힐/실드 (유틸 평가) ──
    private long totalHeal;
    private long totalHealsOnTeammates;
    private long totalDamageShieldedOnTeammates;

    // ── CC ──
    private int timeCCingOthers;
    private int totalTimeCCDealt;

    // ── 골드/생존 ──
    private int goldSpent;
    private int longestTimeSpentLiving;
    private int totalTimeSpentDead;

    // ── 스킬/스펠 사용 횟수 ──
    private int spell1Casts;
    private int spell2Casts;
    private int spell3Casts;
    private int spell4Casts;

    // Riot이 계산해주는 파생 지표. 모드/구버전에 따라 통째로 없을 수 있어 null 허용.
    private Challenges challenges;

    // 아레나(CHERRY) 전용 필드. 협곡 등 다른 모드에서는 Riot이 0/미포함으로 내려준다 → 래퍼로 받아 null 허용.
    private Integer placement;         // 개인 최종 등수
    private Integer subteamPlacement;  // 소속 듀오(2인 팀)의 최종 등수 = 1~4위
    private Integer playerSubteamId;   // 어느 듀오인지 식별(같은 값이면 한 팀)

    // Riot challenges 블록 중 우리가 쓰는 파생 지표만 선별(전체는 100+ 필드).
    @Getter
    @Setter
    public static class Challenges {
        private Double kda;                   // (K+A)/D
        private Double killParticipation;     // 킬관여율 0~1
        private Double teamDamagePercentage;  // 팀 내 딜 비중 0~1
        private Double damagePerMinute;       // 분당 챔피언 딜
        private Double goldPerMinute;         // 분당 골드
        private Double visionScorePerMinute;  // 분당 시야 점수
        private Integer soloKills;            // 솔로킬 수
    }


    @Getter
    @Setter
    public static class Perks {
        private StatPerks statPerks;
        private List<Style> styles; // primaryStyle(주룬), subStyle(보조룬) 2개
    }

    @Getter
    @Setter
    public static class StatPerks {
        private Integer offense;
        private Integer flex;
        private Integer defense;
    }

    @Getter
    @Setter
    public static class Style {
        private String description; // "primaryStyle" or "subStyle"
        private Integer style;      // 룬 계열 ID (예: 8100 지배)
        private List<Selection> selections;
    }

    @Getter
    @Setter
    public static class Selection {
        private Integer perk; // 선택한 룬 ID (selections[0] = 핵심룬)
    }
}
