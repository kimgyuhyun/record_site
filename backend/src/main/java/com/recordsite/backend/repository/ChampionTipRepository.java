package com.recordsite.backend.repository;

import com.recordsite.backend.entity.ChampionTip;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    // 추천/비추천은 값을 읽지 않고 DB에서 바로 증가시킨다. 읽지 않으므로 동시 요청이 서로의 증가분을 덮어쓸 수 없다.
    // 반환값은 갱신된 행 수 — 0 이면 없는 팁이다.
    @Modifying
    @Query("update ChampionTip t set t.upvotes = t.upvotes + 1 where t.id = :id")
    int increaseUpvotes(@Param("id") Long id);

    @Modifying
    @Query("update ChampionTip t set t.downvotes = t.downvotes + 1 where t.id = :id")
    int increaseDownvotes(@Param("id") Long id);

    // 신고는 카운트 증가에 더해 임계값 판정(숨김)까지 하는 read-modify-write 라 단순 증가 쿼리로 못 바꾼다.
    // 판정 규칙을 엔티티에 둔 채로 유실을 막기 위해 행에 쓰기 잠금을 걸어 동시 신고를 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ChampionTip t where t.id = :id")
    Optional<ChampionTip> findByIdForUpdate(@Param("id") Long id);
}
