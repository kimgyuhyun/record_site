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
    private final ParticipantService participantService;
    private final RiotMatchClient riotMatchClient;


    // puuid 기준으로 매치목록 가져옵니다.
    // DB에 정보가 없으면 Riot 호출 후 저장합니다.
    public List<MatchListDto> getMatchListByPuuid(String puuid) {
        List<Participant> participantList = participantRepository.findAllParticipantListByPuuid(puuid);
        List<MatchListDto> matchListDtos = new ArrayList<>();

        List<String> processMatchIds = new ArrayList<>(); //중복 체크용 matchIdList

        if (!participantList.isEmpty()) { // 이 puuid로 저장된 참가자리스트가 있으면
            for (Participant participant : participantList) {
                String riotMatchId = participant.getMatch().getMatchId();

                if (processMatchIds.contains(riotMatchId)) {
                    continue; // 중복이면 스킵하고 다음 루프 진행
                }

                processMatchIds.add(riotMatchId);
                matchListDtos.add(processMatch(puuid, riotMatchId));
            }
        }

        List<String> machIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
        for (String matchId : machIdList) {
            if (processMatchIds.contains(matchId)) {
                continue;
            }
            processMatchIds.add(matchId);
            matchListDtos.add(processMatch(puuid, matchId));
        }
        return matchListDtos;
    }


    private MatchListDto processMatch(String puuid, String riotMatchId) {
        String targetMatchId = riotMatchId;
        Match match = matchRepository.findByMatchId(targetMatchId);
        List<Participant> matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);
        if (matchParticipantList.size() > 10) { // 매치참가자가 10명보다 크면 예외
            throw new IllegalStateException("Participant count > 10. matchId=" + targetMatchId);
        }
        // match가 null 또는 matchParticipantList가 10보다 작으면
        if (match == null || matchParticipantList.size() < 10) {
            RiotMatchResponse res = riotMatchClient.getMatchById(targetMatchId);
            targetMatchId = res.getMetadata().getMatchId(); // 안전하게 라이엇 응답에 matchId로 다시 세팅
           
            match = matchRepository.findByMatchId(targetMatchId); // 다시 가져온 matchId로 재조회
            if (match == null) {
                match = matchRepository.save(Match.from(res));
            }
            
            // 매치 참가자 리스트 재조회
            matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);
            if (matchParticipantList.size() > 10) { // 10보다 크면 예외
                throw new IllegalStateException("Participant count > 10. matchId=" + targetMatchId);
            }
            if (matchParticipantList.size() < 10) {
                for (RiotParticipantResponse rp : res.getInfo().getParticipants()) {
                    boolean exists = participantRepository.existsByMatchAndParticipantId(match, rp.getParticipantId());
                    if (!exists) {
                        participantRepository.save(Participant.from(rp, match));
                    }
                }
            }
            matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);
        }
        // 최종 검증: 반드시 정확히 10명이어야 함
        if (matchParticipantList.size() != 10) {
            throw new IllegalStateException("Participant count != 10. matchId=" + targetMatchId);
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
            throw new IllegalStateException("Requested puuid not found in match participants. matchId=" + targetMatchId);
        }
        participantService.linkSummonerToParticipant(me);
        return MatchListDto.from(match, me, icons);
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