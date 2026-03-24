package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Getter
    @Setter
    public static class Perks {
        private StatPerks statPerks;
    }

    @Getter
    @Setter
    public static class StatPerks {
        private Integer offense;
        private Integer flex;
        private Integer defense;
    }
}
