package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotParticipantResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "participant")
@Getter
@Setter
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summoner_id", nullable = true)
    private Summoner summoner;

    @Column(name = "participant_id", nullable = false)
    private Integer participantId; // 경기 내 참가자 번호

    @Column(nullable = false)
    private String puuid; // 계정 고유 ID

    @Column(name = "game_name", nullable = false)
    private String gameName; // 게임 닉네임

    @Column(name = "tag_line")
    private String tagLine; // 아이디 태그

    @Column(nullable = false)
    private int teamId; // 팀 id 블루인지 레드인지
    @Column(nullable = false)
    private boolean win; // 승리 여부

    @Column(nullable = false)
    private int championId; // 사용 챔피언 ID
    private String championName; // 사용 챔피언 영문명

    @Column(nullable = false)
    private int kills; // 킬 수
    @Column(nullable = false)
    private int deaths; // 데스 수
    @Column(nullable = false)
    private int assists; // 어시 수


    private String teamPosition; // 권장 포지션
    private String individualPosition; // 개인 포지션


    private int item0; // 아이템
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;


    @Column(nullable = false)
    private int spell1; // 스펠
    @Column(nullable = false)
    private int spell2;

    @Column(nullable = false)
    private int goldEarned; // 총 획득 골드

    @Column(nullable = false)
    private Long totalDamageDealt; // 적에게 가한 피해량
    @Column(nullable = false)
    private Long totalDamageDealtToChampions; // 챔피언에게 가한 피해량
    @Column(nullable = false)
    private Long totalDamageTaken; // 받은 피해량    

    @Column(nullable = false)
    private int visionScore; // 시야 점수
    @Column(nullable = false)
    private int championLevel;

    @Column(nullable = false)
    private Integer StatPerkOffense;
    @Column(nullable = false)
    private Integer statPerkFlex;
    @Column(nullable = false)
    private Integer statPerkDefense;

    public static Participant from(RiotParticipantResponse res, Match match) {
        Participant p = new Participant();
        p.setMatch(match);

        p.setParticipantId(res.getParticipantId());
        p.setPuuid(res.getPuuid());
        p.setGameName(res.getRiotIdGameName());
        p.setTagLine(res.getRiotIdTagLine());
        p.setTeamId(res.getTeamId());
        p.setWin(res.isWin());
        p.setChampionId(res.getChampionId());
        p.setChampionName(res.getChampionName());
        p.setKills(res.getKills());
        p.setDeaths(res.getDeaths());
        p.setAssists(res.getAssists());
        p.setTeamPosition(res.getTeamPosition());
        p.setIndividualPosition(res.getIndividualPosition());
        p.setItem0(res.getItem0());
        p.setItem1(res.getItem1());
        p.setItem2(res.getItem2());
        p.setItem3(res.getItem3());
        p.setItem4(res.getItem4());
        p.setItem5(res.getItem5());
        p.setItem6(res.getItem6());
        p.setSpell1(res.getSpell1Id());
        p.setSpell2(res.getSpell2Id());
        p.setGoldEarned(res.getGoldEarned());
        p.setTotalDamageDealt(res.getTotalDamageDealt());
        p.setTotalDamageDealtToChampions(res.getTotalDamageDealtToChampions());
        p.setTotalDamageTaken(res.getTotalDamageTaken());
        p.setVisionScore(res.getVisionScore());
        p.setChampionLevel(res.getChampionLevel());
        p.setStatPerkOffense(res.getStatPerkOffense());
        p.setStatPerkFlex(res.getStatPerkFlex());
        p.setStatPerkDefense(res.getStatPerkDefense());

        return p;
    }

}
