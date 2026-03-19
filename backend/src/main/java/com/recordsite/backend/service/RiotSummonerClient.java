package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotAccountResponse;
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
    private String krBaseUrl;
    // summoner-v4 , match-v5 같은 LOK KR 계열에 사용

    @Value("${riot.api.asia-base-url}")
    private String asiaBaseUrl;
    // account-v1 같은 계정 단위에 씀

    @Value("${riot.api.account-by-riot-id-path}")
    private String accountByRiotIdPath;
    // gameName과 tagLine으로 계정 검색할때 사용 

    @Value("${riot.api.summoner-by-puuid-path}")
    private String summonerByPuuidPath;
    // puuid로 소환사 상세정보 얻을때 사용


    public RiotSummonerResponse getByRiotId(String gameName, String tagLine) {
        String puuid = getPuuidByRiotId(gameName, tagLine); // gameName과 tagLine으로 puuid 얻는 함수
        if (puuid == null || puuid.isBlank()) return null;

        return getSummonerByPuuid(puuid); // puuid로 소환사 상세정보 얻는 함수
    }

    // RiotId=gameName, tagLine 으로 Puuid 가져오는 함수
    private String getPuuidByRiotId(String gameName, String tagLine) {
        String url = asiaBaseUrl + accountByRiotIdPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(gameName, tagLine)
                .toUri();

        RiotAccountResponse res = restTemplate.getForObject(uri, RiotAccountResponse.class);
        return res == null ? null : res.getPuuid();
    }

    // puuid로 Summoner 상세정보 가져오는 함수
    private RiotSummonerResponse getSummonerByPuuid(String puuid) {
        String url = krBaseUrl + summonerByPuuidPath;

        URI uri = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("api_key", apiKey)
                .buildAndExpand(puuid)
                .toUri();

        return restTemplate.getForObject(uri, RiotSummonerResponse.class);
    }

}

//UriComponentsBuilder builder = UriComponentsBuilder // URI 조각들을 다루는 빌더 객체를 생성
//        .fromHttpUrl(url) // 스킴/호스트/경로까지 파싱해서 빌더에 넣음
//        // url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/{name}";
//        // 스킴: https,  호스트: 도메인, 경로: /lol/.../{name}, 쿼리: api_key=YOUR_API_KEY
//        .queryParam("api_key", apiKey);
//
//URI uri = builder.buildAndExpand(name).toUri();
//// 경로에 남아있던 name 자리에 실제 검색할 소환사 이름을 끼움
//        /* "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/
//        hide%20on%20bush?api_key=YOUR_API_KEY 를 최종 URI 객체로 만들어줌
//        */