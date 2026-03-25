package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Summoner findBypuuid(String puuid);
    Summoner findByNameAndTagLine(String name, String tagLine);
    List<Summoner> findAllByName(String name);
}
