package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionRotationDto;
import com.recordsite.backend.service.ChampionRotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/champion-rotation")
@RequiredArgsConstructor
public class ChampionRotationController {

    private final ChampionRotationService championRotationService;

    @GetMapping
    public ResponseEntity<ChampionRotationDto> getCurrentRotation() {
        return ResponseEntity.ok(championRotationService.getCurrentRotation());
    }
}
