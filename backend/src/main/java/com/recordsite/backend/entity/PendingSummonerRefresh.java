package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 매치망 크롤러의 작업 큐 한 건. 검색된 유저의 동료 puuid 를 적재해 백그라운드로 매치를 점진 수집한다.
// puuid 유니크 제약으로 같은 소환사가 큐에 중복 적재되는 것을 막는다.
@Entity
@Table(
        name = "pending_summoner_refresh",
        uniqueConstraints = @UniqueConstraint(name = "uk_pending_puuid", columnNames = "puuid")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingSummonerRefresh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 78)
    private String puuid;

    // 검색된 유저(0)로부터의 확장 단계. depth 가 클수록 원래 검색 유저와 거리가 멀다.
    @Column(nullable = false)
    private int depth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CrawlStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 처리(DONE/FAILED) 시각. 하루 처리량 상한 집계의 기준이 된다. 미처리 시 null.
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private PendingSummonerRefresh(String puuid, int depth) {
        this.puuid = puuid;
        this.depth = depth;
        this.status = CrawlStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static PendingSummonerRefresh pending(String puuid, int depth) {
        return new PendingSummonerRefresh(puuid, depth);
    }

    public void markDone() {
        this.status = CrawlStatus.DONE;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = CrawlStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingSummonerRefresh other)) return false;
        return id != null && id.equals(other.id); // 미저장(id=null) 상태는 동일성 비교 안 함
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // 미저장 상태에서도 Set 오작동 방지를 위한 클래스 기반 고정값
    }
}
