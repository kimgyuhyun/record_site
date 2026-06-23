package com.recordsite.backend.service;

import com.recordsite.backend.config.CacheConfig;
import com.recordsite.backend.dto.ChampionSummaryDto;
import com.recordsite.backend.entity.Champion;
import com.recordsite.backend.exception.ChampionNotFoundException;
import com.recordsite.backend.repository.ChampionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionService {
    private final ChampionRepository championRepository;

    // 정적 챔피언 목록 — 패치 단위로만 바뀌므로 길게 캐싱(12h).
    @Cacheable(CacheConfig.STATIC_CHAMPIONS)
    public List<ChampionSummaryDto> getChampionSummaries() {
        List<Champion> champions = championRepository.findAll();
        List<ChampionSummaryDto> dtos = new ArrayList<>();

        for (Champion champion : champions) {
            ChampionSummaryDto dto = ChampionSummaryDto.from(champion);
            dtos.add(dto);
        }

        return dtos;
    }

    public ChampionSummaryDto getChampionByName(String championId) {
        Champion champion = championRepository.findByChampionId(championId);

        if (champion == null) {
            throw new ChampionNotFoundException(championId);
        }
        return ChampionSummaryDto.from(champion);
    }
}
