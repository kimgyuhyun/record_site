package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionSummaryDto;
import com.recordsite.backend.entity.Champion;
import com.recordsite.backend.repository.ChampionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionService {
    private final ChampionRepository championRepository;



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
        ChampionSummaryDto dto =  ChampionSummaryDto.from(champion);
        return dto;
    }
}
