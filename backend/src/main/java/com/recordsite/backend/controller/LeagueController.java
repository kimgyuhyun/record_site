package com.recordsite.backend.controller;

import com.recordsite.backend.dto.RiotLeagueEntryResponse;
import com.recordsite.backend.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/league")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

//    // puuid로 솔로랭크/자유랭크 정보 조회
//    @GetMapping("/entries")
//    public ResponseEntity<List<RiotLeagueEntryResponse>> getLeagueEntries(
//            @RequestParam String puuid) {
//        return ResponseEntity.ok(leagueService.getLeagueEntriesByPuuid(puuid));
//    }
}
