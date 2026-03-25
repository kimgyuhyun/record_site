package com.recordsite.backend.service;


import com.recordsite.backend.dto.RiotSummonerResponse;
import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.exception.SummonerNotFoundException;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummonerService {
    private final SummonerRepository summonerRepository;
    private final RiotSummonerClient riotSummonerClient;

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
            summoner = summonerRepository.save(summoner);

        }
        return SummonerDto.from(summoner);
    }

    public List<SummonerDto> findSummonerListByName(String name) {
        List<Summoner> summonerList = summonerRepository.findAllByName(name);
        List<SummonerDto> summonerDtoList= new ArrayList<>();
        for (Summoner summoner : summonerList) {
            SummonerDto dto = SummonerDto.from(summoner);
            summonerDtoList.add(dto);
        }

        return summonerDtoList;
    }
}
