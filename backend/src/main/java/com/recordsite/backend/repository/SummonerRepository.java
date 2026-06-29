package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Summoner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Summoner findBypuuid(String puuid);
    Summoner findByNameAndTagLine(String name, String tagLine);
    List<Summoner> findAllByName(String name);
    List<Summoner> findByNameContainingIgnoreCase(String name);

    // 백그라운드 LP 폴링 대상: 아직 추적 기간이 남았고 랭크 티어가 있는 소환사를 최근 조회순으로.
    // Pageable 로 한 주기 폴링 배치 크기를 제한한다.
    @Query("""
            select s.puuid
            from Summoner s
            where s.trackedUntil > :now
            and (s.soloTier is not null or s.flexTier is not null)
            order by s.trackedUntil desc
            """)
    List<String> findTrackedPuuids(@Param("now") LocalDateTime now, Pageable pageable);
}
