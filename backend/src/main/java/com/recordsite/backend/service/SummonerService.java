package com.recordsite.backend.service;


import com.recordsite.backend.dto.RiotSummonerResponse;
import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.exception.SummonerNotFoundException;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummonerService {
    private final SummonerRepository summonerRepository;
    private final RiotSummonerClient riotSummonerClient;

    @Value("${riot.rank-poll.tracking-window-hours:72}")
    private long trackingWindowHours;

    // RiotId는 항상 gameName과 Tagline을 뜻한다.
    public SummonerDto findSummonerByRiotId(String gameName, String tagLine) {
        Summoner summoner = summonerRepository.findByNameAndTagLine(gameName, tagLine);
        if (summoner == null) {
            RiotSummonerResponse res = riotSummonerClient.getSummonerByRiotId(gameName, tagLine);
            if (res == null) {
                throw new SummonerNotFoundException(gameName);
            }
            // return SummonerDto.from(summonerRepository.save(Summoner.from(riotSummonerClient.getByName(name))));
            // 인라인화는 이렇게하면 되지만 가독성을 위해 풀어서 쓰겠음

            summoner = Summoner.from(res);
            summoner.setName(gameName);
            summoner.setTagLine(tagLine);
            // v4 소환사 상세 정보에서는 게임이름과 태그를 안줘서 유저가 전달한 이름과 태그를 여기서 설정해서
            // DB에 최종 저장해야함
        }
        // 프로필을 본 소환사는 일정 기간 추적 대상에 올려, 백그라운드 LP 폴러가 판당 스냅샷을 쌓게 한다.
        summoner.extendTracking(Duration.ofHours(trackingWindowHours));
        summoner = summonerRepository.save(summoner);
        return SummonerDto.from(summoner);
    }

    public List<SummonerDto> searchByName(String name) {
        return summonerRepository
                .findByNameContainingIgnoreCase(name)
                .stream()
                .map(SummonerDto :: from)
                .toList();
    }

    public SummonerDto findByPuuid(String puuid) {
        return SummonerDto.from(summonerRepository.findBypuuid(puuid));
    }
}
