package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @GetMapping("api/summoners/{puuid}/matches")
    public List<MatchSummaryDto> getMatchSummaryList(@PathVariable String puuid) {
        return matchService.getMatchSummaryListByPuuid(puuid);
    }
}
