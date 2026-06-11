package com.recordsite.backend.repository;

import com.recordsite.backend.dto.MatchRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParticipantRepositoryCustom {
    // puuid로 매치목록 페이징 조회
    Page<MatchRecordDto> findMatchRecordByPuuid(String puuid, Pageable pageable);
}
