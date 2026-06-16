package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Match findByMatchId(String matchId);

    @Query("""
            SELECT m.matchId
            FROM Match m
            WHERE m.matchId IN :matchIds
            """)
    Set<String> findExistingMatchIds(@Param("matchIds") List<String> matchIds);

}
