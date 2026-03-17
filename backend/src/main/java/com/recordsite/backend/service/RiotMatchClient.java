package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotMatchResponse;
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

    @Value("${riot.api.match-ids-by-puuid-path}")
    // /lol/match/v5/matches/by-puuid/{puuid}/ids
    private String matchIdsByPuuidPath;

    @Value("${riot.api.match-by-id-path}")
    // /lol/match/v5/matches/{matchId}
    private String matchByIdPath;

    // puuid 로 최근 matchId 리스트 가져오기
    public List<String> getMatchIdsByPuuid(String puuid, int start, int count) {
        String url = baseUrl + matchIdsByPuuidPath;

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("start", start)
                .queryParam("count", count)
                .queryParam("api_key", apiKey);

        URI uri = builder.buildAndExpand(puuid).toUri();

        String[] ids = restTemplate.getForObject(uri, String[].class);
        if (ids == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(ids);
    }

    // matchId 하나로 매치 + 참가자 전체 정보 가져오기
    public RiotMatchResponse getMatchById(String matchId) {
        String url = baseUrl + matchByIdPath;

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey);

        URI uri = builder.buildAndExpand(matchId).toUri();

        return restTemplate.getForObject(uri, RiotMatchResponse.class);
    }

}
