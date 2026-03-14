package com.recordsite.backend.entity;

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
    private Long totalDamageDealtToChampions;

    @Column(nullable = false)
    private int visionScore; // 시야 점수
    @Column(nullable = false)
    private int championLevel;

    private String perks;

}
