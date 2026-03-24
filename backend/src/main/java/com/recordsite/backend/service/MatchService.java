package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchListDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional // 서비스 메서드 실행 동안 영속성 컨텍스트를 유지해줘서 LAZY 로딩이 메서드
// 내부에서 일어나도록 보장
public class MatchService {
    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final RiotMatchClient riotMatchClient;


    // puuid 기준으로 매치목록 가져옵니다.
    // DB에 정보가 없으면 Riot 호출 후 저장합니다.
    public List<MatchListDto> getMatchListByPuuid(String puuid) {
        List<Participant> participantList = participantRepository.findAllParticipantListByPuuid(puuid);
        List<MatchListDto> matchListDtos = new ArrayList<>();

        if (!participantList.isEmpty()) {
            for (Participant participant : participantList) { // 내가 참가한 목록을 하나씩뺌
                Match match = participant.getMatch(); // 참가한 매치를 빼고
                String matchId = match.getMatchId(); // 거기서 매치아이디까지 뺴서 따로 저장해둠

                List<Participant> matchParticipantList = participantRepository.findByMatchIdForParticipantList(matchId);
                // 해당판에 참가한 모든 유저목록을 가져옴
                if (matchParticipantList.isEmpty()) {
                    throw new IllegalStateException(
                            "DB inconsistency: participants exist for puuid but none for matchId=" + matchId);
                }

                List<MatchListDto.ParticipantChampionIcon> icons = new ArrayList<>();
                for (Participant p : matchParticipantList) {
                    icons.add(new MatchListDto.ParticipantChampionIcon(
                            p.getPuuid(),
                            p.getParticipantId(),
                            p.getTeamId(),
                            p.getChampionId(),
                            p.getChampionName(),
                            p.getTeamPosition(),
                            p.getIndividualPosition()
                    ));
                }

                matchListDtos.add(MatchListDto.from(match, participant, icons));
                // 해당 매치, 본인, 나머지 참가자들
            }
            return matchListDtos;
        } else {
            List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
            List<MatchListDto> matchListDtoR = new ArrayList<>();
            for (String matchId : matchIdList) {
                RiotMatchResponse res = riotMatchClient.getMatchById(matchId);
                Match match = matchRepository.save(Match.from(res));
                List<Participant> matchParticipantList = new ArrayList<>();

                for (RiotParticipantResponse rp : res.getInfo().getParticipants()) {
                    Participant pa = participantRepository.save(Participant.from(rp, match));
                    matchParticipantList.add(pa);
                }


                Participant me = null;
                List<MatchListDto.ParticipantChampionIcon> icons = new ArrayList<>();
                for (Participant p : matchParticipantList) {
                    icons.add(new MatchListDto.ParticipantChampionIcon(
                            p.getPuuid(),
                            p.getParticipantId(),
                            p.getTeamId(),
                            p.getChampionId(),
                            p.getChampionName(),
                            p.getTeamPosition(),
                            p.getIndividualPosition()
                    ));

                    if (puuid.equals(p.getPuuid())) {
                        me = p;
                    }
                }

                if (me == null) {
                    throw new IllegalStateException("Requested puuid not found in match participants. matchId=" + matchId);
                }
                matchListDtoR.add(MatchListDto.from(match, me, icons));
            }
            return matchListDtoR;
        }
    }
    
    
    public List<MatchSummaryDto> getMatchSummaryListByMatchId(String matchId) {
        List<Participant> participantList = participantRepository.findByMatchIdForParticipantList(matchId);
        Match match = matchRepository.findByMatchId(matchId);
        List<MatchSummaryDto> matchSummaryDtoList = new ArrayList<>();

        if (match == null) {
            throw new IllegalStateException("Match not found: " + matchId);
        }

        for (Participant participant : participantList) {
            matchSummaryDtoList.add(MatchSummaryDto.from(match, participant));
        }
        return matchSummaryDtoList;
    }

}

//        return participantList.stream()
//                .map(p -> MatchSummaryDto.from(p.getMatch(), p))
//                .toList(); 스트림 버전