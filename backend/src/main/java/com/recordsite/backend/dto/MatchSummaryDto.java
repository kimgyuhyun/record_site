package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchSummaryDto {

    // 매치 공통 정보
    private String matchId;
    private Long gameCreation;
    private Long gameDuration;
    private int queueId;
    private int mapId;
    private String gameMode;
    private String gameType;

    // 소환사 정보, 해당 판 정보
    private String puuid;
    private String gameName;
    private String tagLine;
    private int teamId;
    private boolean win;

    private int championId;
    private String championName;
    private int championLevel;

    private int kills;
    private int deaths;
    private int assists;

    private int goldEarned;
    private Long totalDamageDealt;
    private Long totalDamageDealtToChampions;
    private Long totalDamageTaken;

    private int visionScore;

    // 아이템/스펠/파편
    private int item0;
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;

    private int spell1;
    private int spell2;

    private Integer statPerkOffense;
    private Integer statPerkFlex;
    private Integer statPerkDefense;

    public static MatchSummaryDto from(Match match, Participant p) {
        MatchSummaryDto dto = new MatchSummaryDto();

        // 매치 공통
        dto.setMatchId(match.getMatchId());
        dto.setGameCreation(match.getGameCreation());
        dto.setGameDuration(match.getGameDuration());
        dto.setQueueId(match.getQueueId());
        dto.setMapId(match.getMapId());
        dto.setGameMode(match.getGameMode());
        dto.setGameType(match.getGameType());

        // 개인 정보
        dto.setPuuid(p.getPuuid());
        dto.setGameName(p.getGameName());
        dto.setTagLine(p.getTagLine());
        dto.setTeamId(p.getTeamId());
        dto.setWin(p.isWin());

        dto.setChampionId(p.getChampionId());
        dto.setChampionName(p.getChampionName());
        dto.setChampionLevel(p.getChampionLevel());

        dto.setKills(p.getKills());
        dto.setDeaths(p.getDeaths());
        dto.setAssists(p.getAssists());

        dto.setGoldEarned(p.getGoldEarned());
        dto.setTotalDamageDealt(p.getTotalDamageDealt());
        dto.setTotalDamageDealtToChampions(p.getTotalDamageDealtToChampions());
        dto.setTotalDamageTaken(p.getTotalDamageTaken());

        dto.setVisionScore(p.getVisionScore());

        dto.setItem0(p.getItem0());
        dto.setItem1(p.getItem1());
        dto.setItem2(p.getItem2());
        dto.setItem3(p.getItem3());
        dto.setItem4(p.getItem4());
        dto.setItem5(p.getItem5());
        dto.setItem6(p.getItem6());

        dto.setSpell1(p.getSpell1());
        dto.setSpell2(p.getSpell2());

        dto.setStatPerkOffense(p.getStatPerkOffense());
        dto.setStatPerkFlex(p.getStatPerkFlex());
        dto.setStatPerkDefense(p.getStatPerkDefense());

        return dto;
    }
}