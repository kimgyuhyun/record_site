package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.MatchTimelineDto;
import com.recordsite.backend.service.MatchService;
import com.recordsite.backend.service.MatchTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;
    private final MatchTimelineService matchTimelineService;

    @GetMapping(params = "puuid")
    public ResponseEntity<Page<MatchRecordDto>> getMatchList(
            @RequestParam String puuid, Pageable pageable) throws Exception {
        Page<MatchRecordDto> matchRecordDto = matchService.getMatchRecordsByPuuid(puuid, pageable);
        return ResponseEntity.ok(matchRecordDto);
    }

    @GetMapping("/{matchId}/summary")
    public ResponseEntity<List<MatchSummaryDto>> getMatchSummaryList(@PathVariable String matchId) throws Exception {
        return ResponseEntity.ok(matchService.getParticipantSummaryListByMatchId(matchId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Integer> refreshMatches(@RequestParam String puuid) {
        return ResponseEntity.ok(matchService.refreshMatchesByPuuid(puuid));
    }

    // 타임라인 화면(맵/이벤트/골드 그래프)용 — 열 때 즉석으로 Riot 타임라인을 받아 가공해 반환
    @GetMapping("/{matchId}/timeline")
    public ResponseEntity<MatchTimelineDto> getMatchTimeline(@PathVariable String matchId) {
        return ResponseEntity.ok(matchTimelineService.getTimeline(matchId));
    }
}
