package com.recordsite.backend.repository;

import com.recordsite.backend.entity.LpReading;
import com.recordsite.backend.entity.QueueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LpReadingRepository extends JpaRepository<LpReading, Long> {

    // 쓰기 경로: 직전 측정값과 LP 가 같으면 새 줄을 만들지 않기 위해 큐별 가장 최근 측정값 1건을 본다.
    LpReading findFirstByPuuidAndQueueTypeOrderByReadAtEpochMsDesc(String puuid, QueueType queueType);

    // 읽기 경로: 한 소환사의 모든 큐 측정값을 시간순으로. 큐 분리는 서비스에서 한다.
    List<LpReading> findByPuuidOrderByReadAtEpochMsAsc(String puuid);
}
