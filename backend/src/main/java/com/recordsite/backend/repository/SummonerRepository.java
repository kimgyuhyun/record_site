package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Summoner findBypuuid(String puuid);
    Summoner findByNameAndTagLine(String name, String tagLine);
    List<Summoner> findAllByName(String name);
    List<Summoner> findByNameContainingIgnoreCase(String name);

    // 계정의 puuid 가 Riot 쪽에서 바뀐 경우, 저장된 소환사 행을 새 puuid 로 이관한다(자가치유).
    @Modifying
    @Query("update Summoner s set s.puuid = :newPuuid where s.puuid = :oldPuuid")
    int repointPuuid(@Param("oldPuuid") String oldPuuid, @Param("newPuuid") String newPuuid);
}
