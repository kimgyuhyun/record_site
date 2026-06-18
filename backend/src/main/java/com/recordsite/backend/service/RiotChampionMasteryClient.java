package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotChampionMasteryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

// Riot Champion-Mastery-V4 클라이언트 (summoner-v4와 동일하게 KR 호스트 사용)
@Service
@RequiredArgsConstructor
public class RiotChampionMasteryClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String krBaseUrl;

    @Value("${riot.api.champion-mastery-by-puuid-path}")
    private String masteryByPuuidPath;

    // puuid로 챔피언 숙련도 목록 조회 (Riot이 숙련도 점수 내림차순으로 반환)
    public List<RiotChampionMasteryResponse> getMasteryByPuuid(String puuid) {
        String url = krBaseUrl + masteryByPuuidPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUri();

        RiotChampionMasteryResponse[] body =
                restTemplate.getForObject(uri, RiotChampionMasteryResponse[].class);
        return body == null ? Collections.emptyList() : List.of(body);
    }
}
