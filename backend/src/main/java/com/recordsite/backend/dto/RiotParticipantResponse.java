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
