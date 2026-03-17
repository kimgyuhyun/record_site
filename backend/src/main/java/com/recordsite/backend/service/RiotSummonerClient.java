package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotSummonerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class RiotSummonerClient {
    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.base-url}")
    private String baseUrl;

    @Value("${riot.api.summoner-by-name-path}")
    private String summonerByNamePath;

    public RiotSummonerResponse getByName(String name) {
        String url = baseUrl + summonerByNamePath;

        UriComponentsBuilder builder = UriComponentsBuilder // URI 조각들을 다루는 빌더 객체를 생성
                .fromHttpUrl(url) // 스킴/호스트/경로까지 파싱해서 빌더에 넣음
                // url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/{name}";
                // 스킴: https,  호스트: 도메인, 경로: /lol/.../{name}, 쿼리: api_key=YOUR_API_KEY
                .queryParam("api_key", apiKey);

        URI uri = builder.buildAndExpand(name).toUri();
        // 경로에 남아있던 name 자리에 실제 검색할 소환사 이름을 끼움
        /* "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/
        hide%20on%20bush?api_key=YOUR_API_KEY 를 최종 URI 객체로 만들어줌
        */

        return restTemplate.getForObject(uri, RiotSummonerResponse.class);
        // Riot 서버에서 JSON 응답 받은걸 RiotSummonerResponse 객체로 파싱해서 그대로 리턴
    }
}
