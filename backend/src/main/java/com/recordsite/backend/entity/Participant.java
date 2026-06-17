package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotParticipantResponse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_participant_match_slot", // DB 복합 유니크 제약 이름
                        columnNames = {"match_id", "participant_id"}
                        // match_id + participant_id 조합이 같은 값인 row를
                        // 테이블 전체에서 중복으로 만들지 못하게 막는 제약
                        // 1번째 유저와 2번째 유저가 만나는 판이 있을때
                        // 중복저장을 막기위해서 생성함
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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
    private Integer statPerkOffense;
    @Column(nullable = false)
    private Integer statPerkFlex;
    @Column(nullable = false)
    private Integer statPerkDefense;

    @Column
    private boolean gameEndedInEarlySurrender;
    @Column
    private boolean teamEarlySurrendered;

    @Column(nullable = false)
    private int totalMinionsKilled; // 미니언 킬수
    @Column(nullable = false)
    private int neutralMinionsKilled; // 중립 몬스터 킬수

    @Column(nullable = false)
    private int teamKills; // 소속 팀 전체 킬 합산(킬관여율 계산용)

    public static Participant from(RiotParticipantResponse res, Match match) {
        // 빌더 체이닝 안에서 null 체크 로직은 불가 -> 미리 추출
        int offense = 0;
        int flex = 0;
        int defense = 0;
        if (res.getPerks() != null && res.getPerks().getStatPerks() != null) {
            offense = res.getPerks().getStatPerks().getOffense() != null
                    ? res.getPerks().getStatPerks().getOffense() : 0;
            flex = res.getPerks().getStatPerks().getFlex() != null
                    ? res.getPerks().getStatPerks().getFlex() : 0;
            defense = res.getPerks().getStatPerks().getDefense() != null
                    ? res.getPerks().getStatPerks().getDefense() : 0;
        }


        return Participant.builder()
                .match(match)
                .participantId(res.getParticipantId())
                .puuid(res.getPuuid())
                .gameName(res.getRiotIdGameName())
                .tagLine(res.getRiotIdTagline())
                .teamId(res.getTeamId())
                .win(res.isWin())
                .championId(res.getChampionId())
                .championName(res.getChampionName())
                .kills(res.getKills())
                .deaths(res.getDeaths())
                .assists(res.getAssists())
                .teamPosition(res.getTeamPosition())
                .individualPosition(res.getIndividualPosition())
                .item0(res.getItem0())
                .item1(res.getItem1())
                .item2(res.getItem2())
                .item3(res.getItem3())
                .item4(res.getItem4())
                .item5(res.getItem5())
                .item6(res.getItem6())
                .spell1(res.getSummoner1Id())
                .spell2(res.getSummoner2Id())
                .goldEarned(res.getGoldEarned())
                .totalDamageDealt(res.getTotalDamageDealt())
                .totalDamageDealtToChampions(res.getTotalDamageDealtToChampions())
                .totalDamageTaken(res.getTotalDamageTaken())
                .visionScore(res.getVisionScore())
                .championLevel(res.getChampLevel())
                .gameEndedInEarlySurrender(res.isGameEndedInEarlySurrender())
                .teamEarlySurrendered(res.isTeamEarlySurrendered())
                .totalMinionsKilled(res.getTotalMinionsKilled())
                .neutralMinionsKilled(res.getNeutralMinionsKilled())
                .statPerkOffense(offense)
                .statPerkFlex(flex)
                .statPerkDefense(defense)
                // teamKills는 from()에서 세팅 불가 (팀 전체 합산이라 MatchSaveHelper에서 별도 세팅)
                .build();
    }
}
