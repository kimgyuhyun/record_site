package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotMatchResponse;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matchId", nullable = false, unique = true)
    private String matchId; // 해당 판의 matchId (매치 고유 ID)

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Participant> participantList = new ArrayList<>(); // 참가자 10명의 puuID List

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MatchBan> banList = new ArrayList<>(); // 이 매치에서 밴된 챔피언들 (밴율 집계용)

    @Column
    private Long gameCreation; // 게임 생성 시간

    @Column
    private Long gameDuration; // 게임 길이

    @Column
    private int queueId; // 큐 ID (솔랭, 자랭, 칼바람)

    @Column
    private int mapId; // 맵 ID(소환사의 협곡);

    @Column
    private String gameMode; // 게임 모드

    @Column
    private String gameType; // 게임 타입

    @Column
    private String platformId; // 플랫폼 ID

    @Column
    private String gameVersion; // 패치 버전(예: "14.12.123.4567") — 패치별 통계/필터용

    @Column
    private int blueBaronKills;
    @Column
    private int blueDragonKills;
    @Column
    private int blueTowerKills;
    @Column
    private int blueInhibitorKills; // 억제기
    @Column
    private int blueRiftHeraldKills; // 전령

    @Column
    private int redBaronKills;
    @Column
    private int redDragonKills;
    @Column
    private int redTowerKills;
    @Column
    private int redInhibitorKills;
    @Column
    private int redRiftHeraldKills;

    // ── 팀별 퍼스트 오브젝트 플래그(먼저 차지했는지). 구버전 적재분은 false. ──
    @Column(nullable = false) private boolean blueFirstBlood;
    @Column(nullable = false) private boolean blueFirstTower;
    @Column(nullable = false) private boolean blueFirstDragon;
    @Column(nullable = false) private boolean blueFirstBaron;
    @Column(nullable = false) private boolean blueFirstRiftHerald;
    @Column(nullable = false) private boolean blueFirstInhibitor;

    @Column(nullable = false) private boolean redFirstBlood;
    @Column(nullable = false) private boolean redFirstTower;
    @Column(nullable = false) private boolean redFirstDragon;
    @Column(nullable = false) private boolean redFirstBaron;
    @Column(nullable = false) private boolean redFirstRiftHerald;
    @Column(nullable = false) private boolean redFirstInhibitor;

    public static Match from(RiotMatchResponse res) {
        // 루프 결과를 먼저 변수로 추출 (빌더 체이닝 안에서는 for loop 불가)
        int blueBaronKills = 0, blueDragonKills = 0, blueTowerKills = 0;
        int blueInhibitorKills = 0, blueRiftHeraldKills = 0;
        int redBaronKills = 0, redDragonKills = 0, redTowerKills = 0;
        int redInhibitorKills = 0, redRiftHeraldKills = 0;

        boolean blueFirstBlood = false, blueFirstTower = false, blueFirstDragon = false;
        boolean blueFirstBaron = false, blueFirstRiftHerald = false, blueFirstInhibitor = false;
        boolean redFirstBlood = false, redFirstTower = false, redFirstDragon = false;
        boolean redFirstBaron = false, redFirstRiftHerald = false, redFirstInhibitor = false;


        for (RiotMatchResponse.Info.Team team : res.getInfo().getTeams()) {
            RiotMatchResponse.Info.Team.Objectives obj = team.getObjectives();
            if (obj == null) continue;

            if (team.getTeamId() == 100) {
                blueBaronKills = obj.getBaron() != null ? obj.getBaron().getKills()      : 0;
                blueDragonKills = obj.getDragon() != null ? obj.getDragon().getKills()     : 0;
                blueTowerKills = obj.getTower() != null ? obj.getTower().getKills()      : 0;
                blueInhibitorKills = obj.getInhibitor() != null ? obj.getInhibitor().getKills()  : 0;
                blueRiftHeraldKills = obj.getRiftHerald() != null ? obj.getRiftHerald().getKills() : 0;
                blueFirstBlood = obj.getChampion() != null && obj.getChampion().isFirst();
                blueFirstTower = obj.getTower() != null && obj.getTower().isFirst();
                blueFirstDragon = obj.getDragon() != null && obj.getDragon().isFirst();
                blueFirstBaron = obj.getBaron() != null && obj.getBaron().isFirst();
                blueFirstRiftHerald = obj.getRiftHerald() != null && obj.getRiftHerald().isFirst();
                blueFirstInhibitor = obj.getInhibitor() != null && obj.getInhibitor().isFirst();
            } else {
                redBaronKills = obj.getBaron() != null ? obj.getBaron().getKills()      : 0;
                redDragonKills = obj.getDragon() != null ? obj.getDragon().getKills()     : 0;
                redTowerKills = obj.getTower() != null ? obj.getTower().getKills()      : 0;
                redInhibitorKills = obj.getInhibitor() != null ? obj.getInhibitor().getKills()  : 0;
                redRiftHeraldKills = obj.getRiftHerald() != null ? obj.getRiftHerald().getKills() : 0;
                redFirstBlood = obj.getChampion() != null && obj.getChampion().isFirst();
                redFirstTower = obj.getTower() != null && obj.getTower().isFirst();
                redFirstDragon = obj.getDragon() != null && obj.getDragon().isFirst();
                redFirstBaron = obj.getBaron() != null && obj.getBaron().isFirst();
                redFirstRiftHerald = obj.getRiftHerald() != null && obj.getRiftHerald().isFirst();
                redFirstInhibitor = obj.getInhibitor() != null && obj.getInhibitor().isFirst();
            }
        }


        String platformId = (res.getInfo() != null && res.getInfo().getPlatformId() != null)
                ? res.getInfo().getPlatformId() : "";

        Match match = Match.builder()
                .matchId(res.getMetadata().getMatchId())
                .gameCreation(res.getInfo().getGameCreation())
                .gameDuration(res.getInfo().getGameDuration())
                .queueId(res.getInfo().getQueueId())
                .mapId(res.getInfo().getMapId())
                .gameMode(res.getInfo().getGameMode())
                .gameType(res.getInfo().getGameType())
                .platformId(platformId)
                .gameVersion(res.getInfo().getGameVersion())
                .blueBaronKills(blueBaronKills)
                .blueDragonKills(blueDragonKills)
                .blueTowerKills(blueTowerKills)
                .blueInhibitorKills(blueInhibitorKills)
                .blueRiftHeraldKills(blueRiftHeraldKills)
                .redBaronKills(redBaronKills)
                .redDragonKills(redDragonKills)
                .redTowerKills(redTowerKills)
                .redInhibitorKills(redInhibitorKills)
                .redRiftHeraldKills(redRiftHeraldKills)
                .blueFirstBlood(blueFirstBlood)
                .blueFirstTower(blueFirstTower)
                .blueFirstDragon(blueFirstDragon)
                .blueFirstBaron(blueFirstBaron)
                .blueFirstRiftHerald(blueFirstRiftHerald)
                .blueFirstInhibitor(blueFirstInhibitor)
                .redFirstBlood(redFirstBlood)
                .redFirstTower(redFirstTower)
                .redFirstDragon(redFirstDragon)
                .redFirstBaron(redFirstBaron)
                .redFirstRiftHerald(redFirstRiftHerald)
                .redFirstInhibitor(redFirstInhibitor)
                .build();

        // 양 팀 밴 목록을 평탄화해 자식(MatchBan)으로 연결. championId <= 0 은 '밴 안 함'이라 제외한다.
        for (RiotMatchResponse.Info.Team team : res.getInfo().getTeams()) {
            if (team.getBans() == null) continue;
            for (RiotMatchResponse.Info.Team.Ban ban : team.getBans()) {
                if (ban.getChampionId() > 0) {
                    match.getBanList().add(
                            MatchBan.of(match, ban.getChampionId(), team.getTeamId(), ban.getPickTurn()));
                }
            }
        }

        return match;
    }
}

// int, boolean, long 은 null 처리 불필요
// Integer, Long, String, 이나 중첩객체는 Null 처리 필요
