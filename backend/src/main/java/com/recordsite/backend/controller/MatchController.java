package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchListDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @GetMapping(params = "puuid")
    public ResponseEntity<List<MatchListDto>> getMatchList(@RequestParam String puuid) throws Exception {
        return ResponseEntity.ok(matchService.getMatchListByPuuid(puuid));
    }

    @GetMapping("/{matchId}/summary")
    public ResponseEntity<List<MatchSummaryDto>> getMatchSummaryList(@PathVariable String matchId) throws Exception {
        return ResponseEntity.ok(matchService.getMatchSummaryListByMatchId(matchId));
    }
}
