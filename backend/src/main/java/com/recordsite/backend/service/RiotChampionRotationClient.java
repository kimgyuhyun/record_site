package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotChampionRotationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// Riot Champion-Rotation-V3 클라이언트 (무료 로테이션 챔피언, KR 호스트 사용)
@Service
@RequiredArgsConstructor
public class RiotChampionRotationClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String krBaseUrl;

    // 로컬 yaml에 키가 없어도 동작하도록 기본 경로를 명시한다 (champion-rotations-v3 고정 경로)
    @Value("${riot.api.champion-rotations-path:/lol/platform/v3/champion-rotations}")
    private String championRotationsPath;

    // 현재 무료 로테이션 챔피언 목록 조회
    public RiotChampionRotationResponse getCurrentRotation() {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(krBaseUrl + championRotationsPath)
                .queryParam("api_key", apiKey)
                .build()
                .toUri();

        return restTemplate.getForObject(uri, RiotChampionRotationResponse.class);
    }
}
