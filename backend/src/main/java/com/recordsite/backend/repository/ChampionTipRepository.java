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

    // language/patchVersion 은 null 이면 필터하지 않는다("내 언어만"·"현재 버전만" 토글이 꺼진 상태).
    // 숨김 처리된 팁은 항상 제외.

    // 인기순: 점수(추천-비추천) 높은 순, 동점이면 최신순.
    @Query("""
            select t from ChampionTip t
            where t.championId = :championId and t.hidden = false
              and (:language is null or t.language = :language)
              and (:patchVersion is null or t.patchVersion = :patchVersion)
            order by (t.upvotes - t.downvotes) desc, t.createdAt desc
            """)
    Page<ChampionTip> findPopular(@Param("championId") int championId,
                                  @Param("language") String language,
                                  @Param("patchVersion") String patchVersion,
                                  Pageable pageable);

    // 최신순.
    @Query("""
            select t from ChampionTip t
            where t.championId = :championId and t.hidden = false
              and (:language is null or t.language = :language)
              and (:patchVersion is null or t.patchVersion = :patchVersion)
            order by t.createdAt desc
            """)
    Page<ChampionTip> findRecent(@Param("championId") int championId,
                                 @Param("language") String language,
                                 @Param("patchVersion") String patchVersion,
                                 Pageable pageable);

    // 헤더의 "(N개)" 표기용 — 같은 필터 기준 총 개수.
    @Query("""
            select count(t) from ChampionTip t
            where t.championId = :championId and t.hidden = false
              and (:language is null or t.language = :language)
              and (:patchVersion is null or t.patchVersion = :patchVersion)
            """)
    long countFiltered(@Param("championId") int championId,
                       @Param("language") String language,
                       @Param("patchVersion") String patchVersion);
}
