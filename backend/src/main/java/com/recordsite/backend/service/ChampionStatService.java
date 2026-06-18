package com.recordsite.backend.service;

import com.recordsite.backend.dto.PlayedChampionStatDto;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 소환사가 플레이한 챔피언별 통계 조회 (읽기 전용).
@Service
@RequiredArgsConstructor
public class ChampionStatService {

    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public List<PlayedChampionStatDto> getPlayedChampions(String puuid, QueueType queueType) {
        Integer queueId = queueType == null ? null : queueType.queueId();
        return participantRepository.aggregatePlayedChampions(puuid, queueId).stream()
                .map(PlayedChampionStatDto::from)
                .toList();
    }
}
