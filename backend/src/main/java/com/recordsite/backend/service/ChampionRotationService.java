package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionRotationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 무료 로테이션 챔피언 조회 (Riot 라이브 데이터 프록시, DB 저장 없음)
@Service
@RequiredArgsConstructor
public class ChampionRotationService {

    private final RiotChampionRotationClient riotChampionRotationClient;

    public ChampionRotationDto getCurrentRotation() {
        return ChampionRotationDto.from(riotChampionRotationClient.getCurrentRotation());
    }
}
