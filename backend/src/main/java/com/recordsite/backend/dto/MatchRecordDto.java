package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
public class MatchRecordDto {

    // QueryDSL 전용 생성자 (직접 명시 - 순서 안전 보장)
    public MatchRecordDto(
            String matchId, Long gameCreation, Long gameDuration,
            Integer queueId, Integer mapId,
            String gameMode, String gameType,
            String myPuuid, String myGameName, String myTagLine,
            Integer myTeamId, Boolean myWin,
            Integer myChampionId, String myChampionName, Integer myChampionLevel,
            Integer myKills, Integer myDeaths, Integer myAssists,
            Integer myGoldEarned,
            Long myTotalDamageDealt, Long myTotalDamageDealtToChampions, Long myTotalDamageTaken,
            Integer myVisionScore,
            Integer myItem0, Integer myItem1, Integer myItem2, Integer myItem3,
            Integer myItem4, Integer myItem5, Integer myItem6,
            Integer mySpell1, Integer mySpell2,
            Integer myStatPerkOffense, Integer myStatPerkFlex, Integer myStatPerkDefense,
            Integer myPrimaryStyleId, Integer myKeystoneId, Integer mySubStyleId,
            Boolean myGameEndedInEarlySurrender, Boolean myTeamEarlySurrendered,
            int myTotalMinionsKilled, int myNeutralMinionsKilled,
            int teamKills
    ) {
        this.matchId = matchId;
        this.gameCreation = gameCreation;
        this.gameDuration = gameDuration;
        this.queueId = queueId;
        this.mapId = mapId;
        this.gameMode = gameMode;
        this.gameType = gameType;
        this.myPuuid = myPuuid;
        this.myGameName = myGameName;
        this.myTagLine = myTagLine;
        this.myTeamId = myTeamId;
        this.myWin = myWin;
        this.myChampionId = myChampionId;
        this.myChampionName = myChampionName;
        this.myChampionLevel = myChampionLevel;
        this.myKills = myKills;
        this.myDeaths = myDeaths;
        this.myAssists = myAssists;
        this.myGoldEarned = myGoldEarned;
        this.myTotalDamageDealt = myTotalDamageDealt;
        this.myTotalDamageDealtToChampions = myTotalDamageDealtToChampions;
        this.myTotalDamageTaken = myTotalDamageTaken;
        this.myVisionScore = myVisionScore;
        this.myItem0 = myItem0;
        this.myItem1 = myItem1;
        this.myItem2 = myItem2;
        this.myItem3 = myItem3;
        this.myItem4 = myItem4;
        this.myItem5 = myItem5;
        this.myItem6 = myItem6;
        this.mySpell1 = mySpell1;
        this.mySpell2 = mySpell2;
        this.myStatPerkOffense = myStatPerkOffense;
        this.myStatPerkFlex = myStatPerkFlex;
        this.myStatPerkDefense = myStatPerkDefense;
        this.myPrimaryStyleId = myPrimaryStyleId;
        this.myKeystoneId = myKeystoneId;
        this.mySubStyleId = mySubStyleId;
        this.gameEndedInEarlySurrender = myGameEndedInEarlySurrender;
        this.teamEarlySurrendered = myTeamEarlySurrendered;
        this.myTotalMinionsKilled = myTotalMinionsKilled;
        this.myNeutralMinionsKilled = myNeutralMinionsKilled;
        this.teamKills = teamKills;
    }

    // 매치 메타데이터 정보
    private String matchId;
    private Long gameCreation;
    private Long gameDuration;
    private int queueId;
    private int mapId;
    private String gameMode;
    private String gameType;

    // 해당판의 나에 대한 정보
    private String myPuuid;
    private String myGameName;
    private String myTagLine;
    private int myTeamId;
    private boolean myWin;

    private int myChampionId;
    private String myChampionName;
    private int myChampionLevel;

    private int myKills;
    private int myDeaths;
    private int myAssists;

    private int myGoldEarned;
    private Long myTotalDamageDealt;
    private Long myTotalDamageDealtToChampions;
    private Long myTotalDamageTaken;

    private int myVisionScore;

    private int myItem0;
    private int myItem1;
    private int myItem2;
    private int myItem3;
    private int myItem4;
    private int myItem5;
    private int myItem6;

    private int mySpell1;
    private int mySpell2;

    private Integer myStatPerkOffense;
    private Integer myStatPerkFlex;
    private Integer myStatPerkDefense;

    private Integer myPrimaryStyleId; // 주 룬 계열
    private Integer myKeystoneId;     // 핵심 룬
    private Integer mySubStyleId;     // 보조 룬 계열

    private boolean gameEndedInEarlySurrender; // 다시하기 여부
    private boolean teamEarlySurrendered;

    private int myTotalMinionsKilled;
    private int myNeutralMinionsKilled;

    private int teamKills;

    // Service 레이어에서 주입하는 파생/조합 필드 (Setter만 열어둠)
    @Setter
    private double myKillParticipation;
    @Setter
    private List<ParticipantSummaryDto> participantSummaryDtos;

    // 소환사의 현재 랭크 (해당 큐 기준 - 솔로/자유) - Service에서 주입
    @Setter
    private String myTier; // 예: "MASTER" (랭크 없으면 null)
    @Setter
    private String myRank; // 예: "II" (마스터+ 또는 언랭이면 null)

    // 판당 LP 증감 (스냅샷 비교) - 부호 포함, 계산 불가하면 null - Service에서 주입
    @Setter
    private Integer myLpChange;


    public static MatchRecordDto from(Match match, Participant me) {
        return new MatchRecordDto(
                match.getMatchId(),
                match.getGameCreation(),
                match.getGameDuration(),
                match.getQueueId(),
                match.getMapId(),
                match.getGameMode(),
                match.getGameType(),
                me.getPuuid(),
                me.getGameName(),
                me.getTagLine(),
                me.getTeamId(),
                me.isWin(),
                me.getChampionId(),
                me.getChampionName(),
                me.getChampionLevel(),
                me.getKills(),
                me.getDeaths(),
                me.getAssists(),
                me.getGoldEarned(),
                me.getTotalDamageDealt(),
                me.getTotalDamageDealtToChampions(),
                me.getTotalDamageTaken(),
                me.getVisionScore(),
                me.getItem0(),
                me.getItem1(),
                me.getItem2(),
                me.getItem3(),
                me.getItem4(),
                me.getItem5(),
                me.getItem6(),
                me.getSpell1(),
                me.getSpell2(),
                me.getStatPerkOffense(),
                me.getStatPerkFlex(),
                me.getStatPerkDefense(),
                me.getPrimaryStyleId(),
                me.getKeystoneId(),
                me.getSubStyleId(),
                me.isGameEndedInEarlySurrender(),
                me.isTeamEarlySurrendered(),
                me.getTotalMinionsKilled(),
                me.getNeutralMinionsKilled(),
                me.getTeamKills()
        );
    }
}