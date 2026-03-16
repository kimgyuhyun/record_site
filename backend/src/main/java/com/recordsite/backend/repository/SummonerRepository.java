package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Summoner findByName(String name);
}
