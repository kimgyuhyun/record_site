package com.recordsite.backend.entity;

import com.recordsite.backend.domain.LadderScore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 전적 갱신 시점의 소환사 LP를 "그 시점의 최신 랭크 매치"에 앵커로 박아둔 스냅샷.
// 연속된 두 스냅샷의 ladderScore 차이로 판당 LP 증감을 계산한다(스냅샷 비교 방식).
@Entity
@Table(
        name = "rank_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rank_snapshot",
                columnNames = {"puuid", "queue_type", "anchor_match_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 78)
    private String puuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Column(length = 20)
    private String tier;     // 스냅샷은 랭크가 있을 때만 만들므로 사실상 non-null

    @Column(length = 5)
    private String division; // I~IV, 마스터+는 null

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @Column(name = "ladder_score", nullable = false)
    private int ladderScore;

    @Column(name = "anchor_match_id", nullable = false, length = 40)
    private String anchorMatchId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private RankSnapshot(String puuid, QueueType queueType, String tier, String division,
                         int leaguePoints, String anchorMatchId) {
        this.puuid = puuid;
        this.queueType = queueType;
        this.tier = tier;
        this.division = division;
        this.leaguePoints = leaguePoints;
        this.ladderScore = LadderScore.of(tier, division, leaguePoints);
        this.anchorMatchId = anchorMatchId;
        this.createdAt = LocalDateTime.now();
    }

    public static RankSnapshot of(String puuid, QueueType queueType, String tier, String division,
                                  int leaguePoints, String anchorMatchId) {
        return new RankSnapshot(puuid, queueType, tier, division, leaguePoints, anchorMatchId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankSnapshot other)) return false;
        return id != null && id.equals(other.id); // 미저장(id=null) 상태는 동일성 비교 안 함
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // 미저장 상태에서도 Set 오작동 방지를 위한 클래스 기반 고정값
    }
}
