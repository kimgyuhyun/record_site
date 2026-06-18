package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionMasteryDto;
import com.recordsite.backend.service.ChampionMasteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/champion-mastery")
@RequiredArgsConstructor
public class ChampionMasteryController {

    private final ChampionMasteryService championMasteryService;

    @GetMapping(params = "puuid")
    public ResponseEntity<List<ChampionMasteryDto>> getMastery(
            @RequestParam String puuid,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(championMasteryService.getTopMastery(puuid, limit));
    }
}
