package com.recordsite.backend.service;


import com.recordsite.backend.dto.RiotSummonerResponse;
import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.exception.SummonerNotFoundException;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SummonerService {
    private final SummonerRepository summonerRepository;
    private final RiotSummonerClient riotSummonerClient;

    public SummonerDto findSummonerByName(String name) {
        Summoner summoner = summonerRepository.findByName(name);
        if (summoner == null) {
            RiotSummonerResponse res = riotSummonerClient.getByName(name);
            if (res == null) {
                throw new SummonerNotFoundException(name);
            }
            // return SummonerDto.from(summonerRepository.save(Summoner.from(riotSummonerClient.getByName(name))));
            // 인라인화는 이렇게하면 되지만 가독성을 위해 풀어서 쓰곘음

            summoner = Summoner.from(res);
            summoner = summonerRepository.save(summoner);

        }
        return SummonerDto.from(summoner);
    }
}
