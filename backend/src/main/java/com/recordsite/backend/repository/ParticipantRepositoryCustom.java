package com.recordsite.backend.repository;

import com.recordsite.backend.dto.ChampionPositionAggregate;
import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.PlayedChampionAggregate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ParticipantRepositoryCustom {
    // puuid로 매치목록 페이징 조회
    Page<MatchRecordDto> findMatchRecordByPuuid(String puuid, Pageable pageable);

    // puuid의 챔피언별 통계 집계. queueId가 null이면 전체 큐, 아니면 해당 큐만(솔로 420/자유 440)
    List<PlayedChampionAggregate> aggregatePlayedChampions(String puuid, Integer queueId);

    // 전역 챔피언×포지션 통계 집계(전 소환사). queueId가 null이면 전체 큐, 아니면 해당 큐만.
    // 포지션이 비어있는 행(리메이크/특수 모드 등)은 제외한다.
    List<ChampionPositionAggregate> aggregateChampionStatsByPosition(Integer queueId);
}
