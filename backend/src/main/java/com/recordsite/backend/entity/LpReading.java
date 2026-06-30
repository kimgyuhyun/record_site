package com.recordsite.backend.entity;

import com.recordsite.backend.domain.LadderScore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 전적 갱신/폴링 시점마다 찍는 "그 순간의 LP 측정값" 한 줄. 매치에 묶지 않고 시간순 타임라인으로만 쌓는다.
// 판당 LP 증감은 이 측정값들을 시간순으로 보며, 두 인접 측정 사이에 낀 랭크 게임이 정확히 한 판일 때
// 그 차이(ladderScore 차)를 그 한 판에 귀속해서 구한다(읽기 경로는 LpTimelineService).
// readAtEpochMs 를 epoch millis 로 저장하는 이유: 매치의 gameCreation 도 epoch millis(UTC) 라
// 시간대 변환 없이 그대로 대소 비교해 "측정 사이에 낀 게임"을 가려내기 위함.
@Entity
@Table(
        name = "lp_reading",
        indexes = @Index(
                name = "idx_lp_reading_puuid_queue_read",
                columnList = "puuid, queue_type, read_at_epoch_ms"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LpReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 78)
    private String puuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Column(length = 20)
    private String tier;     // 측정은 랭크가 있을 때만 만들므로 사실상 non-null

    @Column(length = 5)
    private String division; // I~IV, 마스터+는 null

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @Column(name = "ladder_score", nullable = false)
    private int ladderScore;

    @Column(name = "read_at_epoch_ms", nullable = false)
    private long readAtEpochMs;

    private LpReading(String puuid, QueueType queueType, String tier, String division,
                      int leaguePoints, long readAtEpochMs) {
        this.puuid = puuid;
        this.queueType = queueType;
        this.tier = tier;
        this.division = division;
        this.leaguePoints = leaguePoints;
        this.ladderScore = LadderScore.of(tier, division, leaguePoints);
        this.readAtEpochMs = readAtEpochMs;
    }

    public static LpReading of(String puuid, QueueType queueType, String tier, String division,
                               int leaguePoints, long readAtEpochMs) {
        return new LpReading(puuid, queueType, tier, division, leaguePoints, readAtEpochMs);
    }
}
