package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Champion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampionRepository extends JpaRepository<Champion, Long> {

    Champion findByChampionId(String championId);
}
