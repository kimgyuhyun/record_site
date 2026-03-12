package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Champion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampionRepository extends JpaRepository<Champion, Long> {

    boolean existsByChampionId(String championId); // DB에 해당 championId가 이미 있는지 체크 (중복 저장 방지용)

    Champion findAllById(String championId);
}
