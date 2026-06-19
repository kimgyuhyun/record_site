package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 상위 티어 사다리 한 칸의 스냅샷(주기적 갱신). 갱신 시 해당 큐 전체를 지우고 새로 적재한다.
//  - gameName/tagLine 은 League-V4 가 주지 않아 account-v1 으로 해소해 비정규화 저장한다(없으면 null).
@Entity
@Table(
        name = "ladder_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ladder_queue_rank",
                columnNames = {"queue_type", "rank_position"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LadderEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApexTier tier;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition; // 사다리 전체에서의 순위(1부터)

    @Column(nullable = false, length = 78)
    private String puuid;

    @Column(name = "game_name")
    private String gameName;

    @Column(name = "tag_line")
    private String tagLine;

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(name = "refreshed_at", nullable = false)
    private LocalDateTime refreshedAt;

    private LadderEntry(QueueType queueType, ApexTier tier, int rankPosition, String puuid,
                        String gameName, String tagLine, int leaguePoints, int wins, int losses) {
        this.queueType = queueType;
        this.tier = tier;
        this.rankPosition = rankPosition;
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.leaguePoints = leaguePoints;
        this.wins = wins;
        this.losses = losses;
        this.refreshedAt = LocalDateTime.now();
    }

    public static LadderEntry of(QueueType queueType, ApexTier tier, int rankPosition, String puuid,
                                 String gameName, String tagLine, int leaguePoints, int wins, int losses) {
        return new LadderEntry(queueType, tier, rankPosition, puuid, gameName, tagLine, leaguePoints, wins, losses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LadderEntry other)) return false;
        return id != null && id.equals(other.id); // 미저장(id=null) 상태는 동일성 비교 안 함
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // 미저장 상태에서도 Set 오작동 방지를 위한 클래스 기반 고정값
    }
}
