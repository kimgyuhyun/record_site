package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findAllByPuuid(String puuid);
    // puuid 가 같은 모든 참가자 = 그 계정이 참가한 모든 경기
}
