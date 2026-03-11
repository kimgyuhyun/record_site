package com.recordsite.backend.repository;

import com.recordsite.backend.entity.RunePath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RunePathRepository extends JpaRepository<RunePath, Long> {
    
    boolean existsByPathKey(Integer pathKey); // 중복 여부 체크

    Optional<RunePath> findByPathKey(Integer pathKey);
}
