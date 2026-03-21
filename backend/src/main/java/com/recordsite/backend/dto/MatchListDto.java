package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MatchListDto {

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

    // 해당판에 참가한 챔피언 아이콘 보여주기위한 용도
    private List<ParticipantChampionIcon> participants;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParticipantChampionIcon {
        private String puuid;
        private Integer participantId; // 경기 내 참가자 번호
        private int teamId;
        private int championId;
        private String championName;
        private String teamPosition;
        private String individualPosition;
    }

    public static MatchListDto from(Match match, Participant me, List<ParticipantChampionIcon> icons) {
        MatchListDto dto = new MatchListDto();

        dto.setMatchId(match.getMatchId());
        dto.setGameCreation(match.getGameCreation());
        dto.setGameDuration(match.getGameDuration());
        dto.setQueueId(match.getQueueId());
        dto.setMapId(match.getMapId());
        dto.setGameMode(match.getGameMode());
        dto.setGameType(match.getGameType());

        dto.setMyPuuid(me.getPuuid());
        dto.setMyGameName(me.getGameName());
        dto.setMyTagLine(me.getTagLine());
        dto.setMyTeamId(me.getTeamId());
        dto.setMyWin(me.isWin());

        dto.setMyChampionId(me.getChampionId());
        dto.setMyChampionName(me.getChampionName());
        dto.setMyChampionLevel(me.getChampionLevel());

        dto.setMyKills(me.getKills());
        dto.setMyDeaths(me.getDeaths());
        dto.setMyAssists(me.getAssists());

        dto.setMyGoldEarned(me.getGoldEarned());
        dto.setMyTotalDamageDealt(me.getTotalDamageDealt());
        dto.setMyTotalDamageDealtToChampions(me.getTotalDamageDealtToChampions());
        dto.setMyTotalDamageTaken(me.getTotalDamageTaken());

        dto.setMyVisionScore(me.getVisionScore());

        dto.setMyItem0(me.getItem0());
        dto.setMyItem1(me.getItem1());
        dto.setMyItem2(me.getItem2());
        dto.setMyItem3(me.getItem3());
        dto.setMyItem4(me.getItem4());
        dto.setMyItem5(me.getItem5());
        dto.setMyItem6(me.getItem6());

        dto.setMySpell1(me.getSpell1());
        dto.setMySpell2(me.getSpell2());

        dto.setMyStatPerkOffense(me.getStatPerkOffense());
        dto.setMyStatPerkFlex(me.getStatPerkFlex());
        dto.setMyStatPerkDefense(me.getStatPerkDefense());

        dto.setParticipants(icons);
        return dto;
    }
}