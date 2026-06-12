package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSaveHelper {

    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantService participantService;
    private final RiotMatchClient riotMatchClient;

    // matchId 하나를 받아서 Match + Participant 저장
    // REQUIRES_NEW로 매치 단위 독립 트랜잭션 - 실패해도 다른 매치에 영향 없음
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMatchWithParticipants(String matchId, String puuid) {

        // 1. Riot API 호출
        RiotMatchResponse res = riotMatchClient.getMatchById(matchId);
        if (res == null || res.getInfo() == null) {
            throw new IllegalStateException("Riot API 응답이 null. matchId=" + matchId);
        }

        // 2. 참가자 목록 추출
        List<RiotParticipantResponse> participants = res.getInfo().getParticipants();

        // 3. Match 저장
        Match match = matchRepository.save(Match.from(res));

        // 4. Participant 전체 saveAll()로 한 번에 저장
        List<Participant> participantEntities = participants.stream()
                .map(rp -> Participant.from(rp, match))
                .toList();
        participantRepository.saveAll(participantEntities);

        // 5. 내 Participant에 Summoner 연결
        Participant me = participantRepository.findByMatchIdAndPuuid(matchId, puuid);
        if (me != null) {
            participantService.linkSummonerToParticipant(me);
        }
    }
}
