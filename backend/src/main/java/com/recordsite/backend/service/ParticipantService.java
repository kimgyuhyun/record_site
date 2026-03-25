package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final SummonerRepository summonerRepository;

    @Transactional
    public void linkSummonerToParticipant(Participant participant) {
        if (participant.getSummoner() != null) return; // 이미 연결된 경우 방어합니다.

        Summoner s = summonerRepository.findBypuuid(participant.getPuuid());
        if (s == null) return; // db에 없으면 검색되지 않았으니 return 해서 null 유지합니다

        participant.setSummoner(s);
        participantRepository.save(participant);
    }




}
