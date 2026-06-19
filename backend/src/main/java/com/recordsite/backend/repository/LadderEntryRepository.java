package com.recordsite.backend.repository;

import com.recordsite.backend.entity.LadderEntry;
import com.recordsite.backend.entity.QueueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LadderEntryRepository extends JpaRepository<LadderEntry, Long> {

    Page<LadderEntry> findByQueueTypeOrderByRankPositionAsc(QueueType queueType, Pageable pageable);

    // 갱신 시 이름 캐시 캐리오버(신규 진입자만 account-v1 재해소)용으로 기존 큐 전체를 읽는다.
    List<LadderEntry> findByQueueType(QueueType queueType);

    void deleteByQueueType(QueueType queueType);
}
