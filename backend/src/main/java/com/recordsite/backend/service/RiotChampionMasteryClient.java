package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotChampionMasteryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

// Riot Champion-Mastery-V4 클라이언트 (summoner-v4와 동일하게 KR 호스트 사용)
@Slf4j
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

        try {
            RiotChampionMasteryResponse[] body =
                    restTemplate.getForObject(uri, RiotChampionMasteryResponse[].class);
            return body == null ? Collections.emptyList() : List.of(body);
        } catch (HttpStatusCodeException e) {
            // 숙련도는 비필수 위젯이라 Riot 에러(만료 키 403, 레이트리밋 429 등)를 500으로 흘리지 않고
            // 빈 목록으로 degrade 한다. 실제 원인 파악을 위해 업스트림 상태 코드를 남긴다.
            log.warn("Riot 챔피언 숙련도 조회 실패 (status={}) puuid={}", e.getStatusCode(), puuid);
            return Collections.emptyList();
        }
    }
}
