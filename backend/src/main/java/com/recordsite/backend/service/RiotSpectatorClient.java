package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotActiveGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// Spectator-V5 클라이언트 (summoner-v4와 동일하게 플랫폼 호스트 = KR 사용)
@Service
@RequiredArgsConstructor
public class RiotSpectatorClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String krBaseUrl;

    @Value("${riot.api.active-game-by-puuid-path}")
    private String activeGameByPuuidPath;

    // 진행 중인 게임 정보 조회. 게임 중이 아니면 Riot이 404를 주므로 null로 변환해 "관전 불가"를 표현한다.
    public RiotActiveGameResponse getActiveGameByPuuid(String puuid) {
        String url = krBaseUrl + activeGameByPuuidPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUri();

        try {
            return restTemplate.getForObject(uri, RiotActiveGameResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null; // 현재 인게임이 아님
        }
    }
}
