package com.recordsite.backend.repository;

import com.recordsite.backend.entity.CrawlStatus;
import com.recordsite.backend.entity.PendingSummonerRefresh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingSummonerRefreshRepository extends JpaRepository<PendingSummonerRefresh, Long> {

    boolean existsByPuuid(String puuid);

    // 큐에서 가장 먼저 들어온 미처리 작업 1건을 꺼낸다(FIFO).
    Optional<PendingSummonerRefresh> findFirstByStatusOrderByIdAsc(CrawlStatus status);

    // 하루 처리량 상한 집계용 — 지정 시각 이후 처리(DONE/FAILED)된 건수.
    long countByProcessedAtAfter(LocalDateTime since);
}
