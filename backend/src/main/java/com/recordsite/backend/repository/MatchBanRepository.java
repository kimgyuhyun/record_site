package com.recordsite.backend.repository;

import com.recordsite.backend.dto.ChampionBanCount;
import com.recordsite.backend.entity.MatchBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchBanRepository extends JpaRepository<MatchBan, Long> {

    // 챔피언별 밴된 매치 수 집계(밴율 분자). queueId 가 null 이면 전체 큐 기준.
    @Query("""
            select new com.recordsite.backend.dto.ChampionBanCount(b.championId, count(b))
            from MatchBan b
            where (:queueId is null or b.match.queueId = :queueId)
            group by b.championId
            """)
    List<ChampionBanCount> aggregateBanCounts(@Param("queueId") Integer queueId);
}
