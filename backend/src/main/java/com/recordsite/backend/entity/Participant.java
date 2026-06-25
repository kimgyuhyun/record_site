package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotParticipantResponse;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    private Integer primaryStyleId; // 주 룬 계열 (예: 8100 지배)
    private Integer keystoneId;     // 핵심 룬 (예: 8112 감전)
    private Integer subStyleId;     // 보조 룬 계열 (예: 8000 정밀)

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

    // 아레나(CHERRY) 전용. 다른 모드 매치에서는 null(래퍼 타입으로 비-아레나 구분).
    private Integer placement;         // 개인 최종 등수
    private Integer subteamPlacement;  // 듀오(2인 팀)의 최종 등수 = 1~4위
    private Integer playerSubteamId;   // 듀오 식별자(같은 값이면 한 팀)

    // ── 멀티킬/연속킬 ── (구버전 적재분은 0으로 채워짐 → 원시 타입 + DEFAULT 0)
    @Column(nullable = false) private int doubleKills;
    @Column(nullable = false) private int tripleKills;
    @Column(nullable = false) private int quadraKills;
    @Column(nullable = false) private int pentaKills;
    @Column(nullable = false) private int largestMultiKill;
    @Column(nullable = false) private int largestKillingSpree;

    // ── 퍼스트(개인) ──
    @Column(nullable = false) private boolean firstBloodKill;
    @Column(nullable = false) private boolean firstTowerKill;

    // ── 시야/와드 상세 ──
    @Column(nullable = false) private int wardsPlaced;
    @Column(nullable = false) private int wardsKilled;
    @Column(name = "vision_wards_bought", nullable = false) private int visionWardsBoughtInGame; // 제어 와드 구매 수
    @Column(nullable = false) private int detectorWardsPlaced;

    // ── 힐/실드(유틸 평가) ──
    @Column(nullable = false) private long totalHeal;
    @Column(nullable = false) private long totalHealsOnTeammates;
    @Column(nullable = false) private long totalDamageShieldedOnTeammates;

    // ── CC ──
    @Column(name = "time_ccing_others", nullable = false) private int timeCCingOthers;
    @Column(name = "total_time_cc_dealt", nullable = false) private int totalTimeCCDealt;

    // ── 골드/생존 ──
    @Column(nullable = false) private int goldSpent;
    @Column(nullable = false) private int longestTimeSpentLiving;
    @Column(nullable = false) private int totalTimeSpentDead;

    // ── 스킬/스펠 사용 횟수 ── (숫자+대문자 경계라 스네이크 변환이 모호 → 컬럼명 명시)
    @Column(name = "spell1_casts", nullable = false) private int spell1Casts;
    @Column(name = "spell2_casts", nullable = false) private int spell2Casts;
    @Column(name = "spell3_casts", nullable = false) private int spell3Casts;
    @Column(name = "spell4_casts", nullable = false) private int spell4Casts;

    // ── Riot 파생 지표(challenges). 모드/구버전에 따라 없을 수 있어 nullable 래퍼. ──
    private Double kda;
    private Double killParticipation;     // 킬관여율 0~1
    private Double teamDamagePercentage;  // 팀 내 딜 비중 0~1
    private Double damagePerMinute;
    private Double goldPerMinute;
    private Double visionScorePerMinute;
    private Integer soloKills;

    // ── 룬 페이지 전체(키스톤/보조계열 외 나머지). 룬 없는 모드는 null 유지. ──
    private Integer primaryRune1; // 주룬 트리 2번째(키스톤 다음)
    private Integer primaryRune2;
    private Integer primaryRune3;
    private Integer subRune1;      // 보조 트리 1번째
    private Integer subRune2;

    // ── 타임라인에서 추출한 빌드 순서. 타임라인 호출 실패/구버전 적재분은 null. ──
    // skillBuildOrder: 레벨업 순서를 Q/W/E/R 로 이어 붙인 문자열(예: "QWEQQR...") — 진화 제외
    @Column(name = "skill_build_order", length = 32)
    private String skillBuildOrder;
    // itemBuildOrder: 구매 아이템 id 를 구매 순서대로 콤마로 이어 붙인 문자열(소모품/장신구 포함)
    @Column(name = "item_build_order", length = 1024)
    private String itemBuildOrder;

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

        // 룬 페이지 추출 (perks.styles가 없는 봇전 등은 null 유지)
        RuneSelection runes = RuneSelection.from(res.getPerks());

        // challenges 블록은 모드/구버전에 따라 통째로 없을 수 있음 → null 가드
        RiotParticipantResponse.Challenges ch = res.getChallenges();

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
                .placement(res.getPlacement())
                .subteamPlacement(res.getSubteamPlacement())
                .playerSubteamId(res.getPlayerSubteamId())
                .statPerkOffense(offense)
                .statPerkFlex(flex)
                .statPerkDefense(defense)
                .primaryStyleId(runes.primaryStyleId())
                .keystoneId(runes.keystoneId())
                .primaryRune1(runes.primaryRune1())
                .primaryRune2(runes.primaryRune2())
                .primaryRune3(runes.primaryRune3())
                .subStyleId(runes.subStyleId())
                .subRune1(runes.subRune1())
                .subRune2(runes.subRune2())
                // 멀티킬/연속킬
                .doubleKills(res.getDoubleKills())
                .tripleKills(res.getTripleKills())
                .quadraKills(res.getQuadraKills())
                .pentaKills(res.getPentaKills())
                .largestMultiKill(res.getLargestMultiKill())
                .largestKillingSpree(res.getLargestKillingSpree())
                // 퍼스트
                .firstBloodKill(res.isFirstBloodKill())
                .firstTowerKill(res.isFirstTowerKill())
                // 시야/와드
                .wardsPlaced(res.getWardsPlaced())
                .wardsKilled(res.getWardsKilled())
                .visionWardsBoughtInGame(res.getVisionWardsBoughtInGame())
                .detectorWardsPlaced(res.getDetectorWardsPlaced())
                // 힐/실드
                .totalHeal(res.getTotalHeal())
                .totalHealsOnTeammates(res.getTotalHealsOnTeammates())
                .totalDamageShieldedOnTeammates(res.getTotalDamageShieldedOnTeammates())
                // CC
                .timeCCingOthers(res.getTimeCCingOthers())
                .totalTimeCCDealt(res.getTotalTimeCCDealt())
                // 골드/생존
                .goldSpent(res.getGoldSpent())
                .longestTimeSpentLiving(res.getLongestTimeSpentLiving())
                .totalTimeSpentDead(res.getTotalTimeSpentDead())
                // 스킬/스펠 사용 횟수
                .spell1Casts(res.getSpell1Casts())
                .spell2Casts(res.getSpell2Casts())
                .spell3Casts(res.getSpell3Casts())
                .spell4Casts(res.getSpell4Casts())
                // Riot 파생 지표(없으면 null)
                .kda(ch == null ? null : ch.getKda())
                .killParticipation(ch == null ? null : ch.getKillParticipation())
                .teamDamagePercentage(ch == null ? null : ch.getTeamDamagePercentage())
                .damagePerMinute(ch == null ? null : ch.getDamagePerMinute())
                .goldPerMinute(ch == null ? null : ch.getGoldPerMinute())
                .visionScorePerMinute(ch == null ? null : ch.getVisionScorePerMinute())
                .soloKills(ch == null ? null : ch.getSoloKills())
                // teamKills는 from()에서 세팅 불가 (팀 전체 합산이라 MatchSaveHelper에서 별도 세팅)
                .build();
    }

    // Riot perks.styles → 룬 페이지 전체(주룬 계열/키스톤+3, 보조 계열/2) 추출 전용 값 객체
    private record RuneSelection(
            Integer primaryStyleId, Integer keystoneId,
            Integer primaryRune1, Integer primaryRune2, Integer primaryRune3,
            Integer subStyleId, Integer subRune1, Integer subRune2) {

        private static final RuneSelection EMPTY =
                new RuneSelection(null, null, null, null, null, null, null, null);

        static RuneSelection from(RiotParticipantResponse.Perks perks) {
            if (perks == null || perks.getStyles() == null) return EMPTY;

            Integer primaryStyleId = null, keystoneId = null;
            Integer primaryRune1 = null, primaryRune2 = null, primaryRune3 = null;
            Integer subStyleId = null, subRune1 = null, subRune2 = null;

            for (RiotParticipantResponse.Style style : perks.getStyles()) {
                if (style == null || style.getDescription() == null) continue;

                if ("primaryStyle".equals(style.getDescription())) {
                    primaryStyleId = style.getStyle();
                    keystoneId   = perk(style, 0); // selections[0] = 핵심룬(키스톤)
                    primaryRune1 = perk(style, 1);
                    primaryRune2 = perk(style, 2);
                    primaryRune3 = perk(style, 3);
                } else if ("subStyle".equals(style.getDescription())) {
                    subStyleId = style.getStyle();
                    subRune1 = perk(style, 0); // 보조 트리는 2개 선택
                    subRune2 = perk(style, 1);
                }
            }
            return new RuneSelection(primaryStyleId, keystoneId,
                    primaryRune1, primaryRune2, primaryRune3, subStyleId, subRune1, subRune2);
        }

        // selections[idx]의 perk id (없으면 null)
        private static Integer perk(RiotParticipantResponse.Style style, int idx) {
            List<RiotParticipantResponse.Selection> sel = style.getSelections();
            if (sel == null || sel.size() <= idx) return null;
            return sel.get(idx).getPerk();
        }
    }
}
