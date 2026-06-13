package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotLeagueEntryResponse;
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
public class RiotLeagueClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String baseUrl;

    @Value("${riot.api.league-entries-by-puuid-path}")
    private String leagueEntriesByPuuidPath;

    // puuid로 솔로랭크/자유랭크 정보 가져오기
    public List<RiotLeagueEntryResponse> getLeagueEntriesByPuuid(String puuid) {
        String url = baseUrl + leagueEntriesByPuuidPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUri();

        RiotLeagueEntryResponse[] entries = restTemplate.getForObject(uri, RiotLeagueEntryResponse[].class);
        if (entries == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(entries);
    }
}
