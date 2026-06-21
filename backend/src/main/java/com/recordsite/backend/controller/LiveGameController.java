package com.recordsite.backend.controller;

import com.recordsite.backend.dto.LiveGameDto;
import com.recordsite.backend.service.LiveGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live-game")
@RequiredArgsConstructor
public class LiveGameController {

    private final LiveGameService liveGameService;

    // 인게임이면 200 + 게임 정보, 아니면 204(No Content).
    @GetMapping(params = "puuid")
    public ResponseEntity<LiveGameDto> getLiveGame(@RequestParam String puuid) {
        LiveGameDto liveGame = liveGameService.getCurrentGame(puuid);
        return liveGame == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(liveGame);
    }
}
