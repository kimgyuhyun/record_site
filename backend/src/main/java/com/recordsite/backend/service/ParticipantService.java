package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final RiotMatchClient riotMatchClient;

    public List<Participant> getParticipantBypuuid(String puuid) {
        List<Participant> participantList = participantRepository.findAllByPuuid(puuid);

        if (!participantList.isEmpty()) {
            return participantList;
        }

        List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
        if (matchIdList.isEmpty()) {
            return Collections.emptyList();
        }

        for (String matchId : matchIdList) {
            RiotMatchResponse riotMatchDetail = riotMatchClient.getMatchById(matchId);

            Match match = Match.from(riotMatchDetail);
            matchRepository.save(match);

           for (RiotParticipantResponse rp : riotMatchDetail.getParticipantList()) {
               participantRepository.save(Participant.from(rp, match));
           }
        }

        return participantRepository.findAllByPuuid(puuid);
    }
}
