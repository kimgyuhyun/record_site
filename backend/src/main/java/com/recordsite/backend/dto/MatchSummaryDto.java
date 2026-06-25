package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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

    private Integer primaryStyleId; // 주 룬 계열
    private Integer keystoneId;     // 핵심 룬
    private Integer subStyleId;     // 보조 룬 계열

    private boolean gameEndedInEarlySurrender;
    private boolean teamEarlySurrendered;

    private int totalMinionsKilled;
    private int neutralMinionsKilled;

    // 아레나(CHERRY) 전용 — 비-아레나 매치에서는 null
    private Integer placement;
    private Integer subteamPlacement;
    private Integer playerSubteamId;

    private Integer blueBaronKills;
    private Integer blueDragonKills;
    private int blueTowerKills;
    private int blueInhibitorKills; // 억제기
    private Integer blueRiftHeraldKills; // 전령
    private Integer blueHordeKills; // 공허 유충

    private Integer redBaronKills;
    private Integer redDragonKills;
    private int redTowerKills;
    private int redInhibitorKills;
    private Integer redRiftHeraldKills;
    private Integer redHordeKills; // 공허 유충

    public static MatchSummaryDto from(Match match, Participant p) {
        return MatchSummaryDto.builder()
                .matchId(match.getMatchId())
                .gameCreation(match.getGameCreation())
                .gameDuration(match.getGameDuration())
                .queueId(match.getQueueId())
                .mapId(match.getMapId())
                .gameMode(match.getGameMode())
                .gameType(match.getGameType())
                .puuid(p.getPuuid())
                .gameName(p.getGameName())
                .tagLine(p.getTagLine())
                .teamId(p.getTeamId())
                .win(p.isWin())
                .championId(p.getChampionId())
                .championName(p.getChampionName())
                .championLevel(p.getChampionLevel())
                .kills(p.getKills())
                .deaths(p.getDeaths())
                .assists(p.getAssists())
                .goldEarned(p.getGoldEarned())
                .totalDamageDealt(p.getTotalDamageDealt())
                .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                .totalDamageTaken(p.getTotalDamageTaken())
                .visionScore(p.getVisionScore())
                .item0(p.getItem0())
                .item1(p.getItem1())
                .item2(p.getItem2())
                .item3(p.getItem3())
                .item4(p.getItem4())
                .item5(p.getItem5())
                .item6(p.getItem6())
                .spell1(p.getSpell1())
                .spell2(p.getSpell2())
                .statPerkOffense(p.getStatPerkOffense())
                .statPerkFlex(p.getStatPerkFlex())
                .statPerkDefense(p.getStatPerkDefense())
                .primaryStyleId(p.getPrimaryStyleId())
                .keystoneId(p.getKeystoneId())
                .subStyleId(p.getSubStyleId())
                .gameEndedInEarlySurrender(p.isGameEndedInEarlySurrender())
                .teamEarlySurrendered(p.isTeamEarlySurrendered())
                .totalMinionsKilled(p.getTotalMinionsKilled())
                .neutralMinionsKilled(p.getNeutralMinionsKilled())
                .placement(p.getPlacement())
                .subteamPlacement(p.getSubteamPlacement())
                .playerSubteamId(p.getPlayerSubteamId())
                .blueBaronKills(match.getBlueBaronKills())
                .blueDragonKills(match.getBlueDragonKills())
                .blueTowerKills(match.getBlueTowerKills())
                .blueInhibitorKills(match.getBlueInhibitorKills())
                .blueRiftHeraldKills(match.getBlueRiftHeraldKills())
                .blueHordeKills(match.getBlueHordeKills())
                .redBaronKills(match.getRedBaronKills())
                .redDragonKills(match.getRedDragonKills())
                .redTowerKills(match.getRedTowerKills())
                .redInhibitorKills(match.getRedInhibitorKills())
                .redRiftHeraldKills(match.getRedRiftHeraldKills())
                .redHordeKills(match.getRedHordeKills())
                .build();
    }
}