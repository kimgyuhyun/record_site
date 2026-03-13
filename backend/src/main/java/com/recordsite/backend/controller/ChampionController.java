package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionSummaryDto;
import com.recordsite.backend.service.ChampionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/champions")
@RequiredArgsConstructor
public class ChampionController {
    private final ChampionService championService;

    @GetMapping
    public List<ChampionSummaryDto> getChampionList() throws Exception {
//        List<ChampionSummaryDto> championSummaries = championService.getChampionSummaries();
//        return championSummaries;
        return championService.getChampionSummaries();
    }

    @GetMapping(params = "name")
    public ChampionSummaryDto getChampionByName(@RequestParam String name) throws Exception{
        return championService.getChampionByName(name);
    }
}
