package com.recordsite.backend.repository;

import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.entity.RankSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankSnapshotRepository extends JpaRepository<RankSnapshot, Long> {

    // 같은 매치에 이미 스냅샷이 박혀 있는지 (갱신 중복 저장 방지)
    boolean existsByPuuidAndQueueTypeAndAnchorMatchId(
            String puuid, QueueType queueType, String anchorMatchId);

    // 읽기 경로에서 인접 차이로 LP 증감을 구하기 위해 시간순 전체 조회
    List<RankSnapshot> findByPuuidOrderByCreatedAtAsc(String puuid);
}
