package com.recordsite.backend.controller;

import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summoners")
@RequiredArgsConstructor
public class SummonerController {
    private final SummonerService summonerService;

    @GetMapping(params = "name")
    public ResponseEntity<SummonerDto> getSummonerByName(@RequestParam String name) {
        return ResponseEntity.ok(summonerService.findSummonerByName(name));
    }
}
