package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionTierRowDto;
import com.recordsite.backend.dto.PlayedChampionStatDto;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.service.ChampionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/champion-stats")
@RequiredArgsConstructor
public class ChampionStatController {

    private final ChampionStatService championStatService;

    // queueType 미지정 = 전체, SOLO = 솔로랭크, FLEX = 자유랭크
    @GetMapping(params = "puuid")
    public ResponseEntity<List<PlayedChampionStatDto>> getPlayedChampions(
            @RequestParam String puuid,
            @RequestParam(required = false) QueueType queueType) {
        return ResponseEntity.ok(championStatService.getPlayedChampions(puuid, queueType));
    }

    // 전역 챔피언 티어 리스트(챔피언 분석 페이지). queueType 미지정 = 전체 큐.
    @GetMapping("/tier-list")
    public ResponseEntity<List<ChampionTierRowDto>> getChampionTierList(
            @RequestParam(required = false) QueueType queueType) {
        return ResponseEntity.ok(championStatService.getChampionTierList(queueType));
    }
}
