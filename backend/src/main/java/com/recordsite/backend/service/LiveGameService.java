package com.recordsite.backend.service;

import com.recordsite.backend.dto.LiveGameDto;
import com.recordsite.backend.dto.RiotActiveGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 인게임(실시간) 정보 조회 (Riot 라이브 데이터 프록시, DB 저장 없음)
@Service
@RequiredArgsConstructor
public class LiveGameService {

    private final RiotSpectatorClient riotSpectatorClient;

    // 현재 진행 중인 게임. 인게임이 아니면 null.
    public LiveGameDto getCurrentGame(String puuid) {
        RiotActiveGameResponse res = riotSpectatorClient.getActiveGameByPuuid(puuid);
        return res == null ? null : LiveGameDto.from(res);
    }
}
