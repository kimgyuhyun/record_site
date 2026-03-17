package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final ParticipantService participantService;

    public List<MatchSummaryDto> getMatchSummaryListByPuuid(String puuid) {
        List<MatchSummaryDto> matchSummaryDtoList = new ArrayList<>();
        List<Participant> participantList = participantService.getParticipantBypuuid(puuid);


        for (Participant p : participantList) {
            Match match = p.getMatch();
            MatchSummaryDto dto = MatchSummaryDto.from(match, p);
            matchSummaryDtoList.add(dto);
        }

        return matchSummaryDtoList;
//        return participantList.stream()
//                .map(p -> MatchSummaryDto.from(p.getMatch(), p))
//                .toList(); 스트림 버전
    }
}
