package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Rune;
import com.recordsite.backend.entity.RunePath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuneRepository extends JpaRepository<Rune, Long> {

    boolean existsByRuneKey(Integer runeKey);
}
