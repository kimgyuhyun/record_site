package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionMasteryDto;
import com.recordsite.backend.dto.RiotChampionMasteryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

// 챔피언 숙련도 조회 (Riot 라이브 데이터 프록시, DB 저장 없음)
@Service
@RequiredArgsConstructor
public class ChampionMasteryService {

    private static final int DEFAULT_LIMIT = 12; // 상위 N개만 노출

    private final RiotChampionMasteryClient riotChampionMasteryClient;

    public List<ChampionMasteryDto> getTopMastery(String puuid, int limit) {
        int topN = limit > 0 ? limit : DEFAULT_LIMIT;
        return riotChampionMasteryClient.getMasteryByPuuid(puuid).stream()
                .sorted(Comparator.comparingLong(RiotChampionMasteryResponse::getChampionPoints).reversed())
                .limit(topN)
                .map(ChampionMasteryDto::from)
                .toList();
    }
}
