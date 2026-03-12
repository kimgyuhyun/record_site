package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Rune;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RuneRepository extends JpaRepository<Rune, Long> {

    boolean existsByRuneKey(Integer runeKey);
}
