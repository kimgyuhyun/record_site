package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionSummaryDto;
import com.recordsite.backend.service.ChampionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<ChampionSummaryDto>> getChampionList() throws Exception {
        return ResponseEntity.ok(championService.getChampionSummaries());
    }

    @GetMapping(params = "name")
    public ResponseEntity<ChampionSummaryDto> getChampionByName(@RequestParam String name) throws Exception{
        return ResponseEntity.ok(championService.getChampionByName(name));
    }
}
