package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotMatchTimelineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiotMatchClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String baseUrl;

    @Value("${riot.api.asia-base-url}")
    private String asiaBaseUrl;
    // account-v1 같은 계정 단위에 씀

    @Value("${riot.api.match-ids-by-puuid-path}")
    // /lol/match/v5/matches/by-puuid/{puuid}/ids
    private String matchIdsByPuuidPath;

    @Value("${riot.api.match-by-id-path}")
    // /lol/match/v5/matches/{matchId}
    private String matchByIdPath;

    @Value("${riot.api.match-timeline-by-id-path}")
    // /lol/match/v5/matches/{matchId}/timeline
    private String matchTimelineByIdPath;

    // puuid 로 최근 matchId 리스트 가져오기
    public List<String> getMatchIdsByPuuid(String puuid, int start, int count) {
        String url = asiaBaseUrl + matchIdsByPuuidPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("start", start)
                .queryParam("count", count)
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUri();

        String[] ids = restTemplate.getForObject(uri, String[].class);
        if (ids == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(ids);
    }

    // matchId 하나로 루트 매치부터 그 안에 metadata, info를 가져옴
    public RiotMatchResponse getMatchById(String matchId) {
        String url = asiaBaseUrl + matchByIdPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(matchId)
                .toUri();

        return restTemplate.getForObject(uri, RiotMatchResponse.class);
    }

    // matchId 하나로 타임라인(분당 프레임 + 이벤트) 조회 → 아이템 구매/스킬 선마 순서 추출용
    public RiotMatchTimelineResponse getMatchTimelineById(String matchId) {
        String url = asiaBaseUrl + matchTimelineByIdPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(matchId)
                .toUri();

        return restTemplate.getForObject(uri, RiotMatchTimelineResponse.class);
    }

}
