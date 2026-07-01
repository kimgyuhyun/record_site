package com.recordsite.backend.repository;

import com.recordsite.backend.entity.ChampionTip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChampionTipRepository extends JpaRepository<ChampionTip, Long> {

    // 인기순: 점수(추천-비추천) 높은 순, 동점이면 최신순. 숨김 처리된 팁은 제외.
    @Query("""
            select t from ChampionTip t
            where t.championId = :championId and t.hidden = false
            order by (t.upvotes - t.downvotes) desc, t.createdAt desc
            """)
    Page<ChampionTip> findPopular(@Param("championId") int championId, Pageable pageable);

    // 최신순. 숨김 처리된 팁은 제외.
    Page<ChampionTip> findByChampionIdAndHiddenFalseOrderByCreatedAtDesc(int championId, Pageable pageable);

    // 헤더의 "(N개)" 표기용 — 숨김 제외 총 개수.
    long countByChampionIdAndHiddenFalse(int championId);
}
