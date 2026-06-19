package com.recordsite.backend.service;

import com.recordsite.backend.entity.LadderEntry;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.LadderEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 사다리 스냅샷의 원자적 교체(기존 큐 전체 삭제 → 신규 적재) 전담.
//  - 스케줄러 스레드에는 트랜잭션이 없다(OSIV는 웹 요청에만 적용). 그래서 삭제+삽입을 한 트랜잭션으로 묶어야 하는데,
//    같은 빈 안에서 @Transactional 메서드를 호출하면 프록시가 적용되지 않으므로(self-invocation) RankingService와 분리한다.
@Component
@RequiredArgsConstructor
public class LadderSnapshotWriter {

    private final LadderEntryRepository ladderEntryRepository;

    @Transactional
    public void replaceSnapshot(QueueType queueType, List<LadderEntry> entries) {
        ladderEntryRepository.deleteByQueueType(queueType);
        // 삭제를 먼저 flush 해 DB에 반영한다. JPA 기본 flush 순서는 INSERT가 DELETE보다 앞이라,
        // flush 없이 두면 신규 행이 기존 행보다 먼저 적재되어 (queue_type, rank_position) 유니크 제약을 위반한다.
        ladderEntryRepository.flush();
        ladderEntryRepository.saveAll(entries);
    }
}
