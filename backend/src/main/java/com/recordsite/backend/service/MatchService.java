package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {
    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantService participantService;
    private final RiotMatchClient riotMatchClient;


    // 게임 모드별 기대 참가자 수
    // CHERRY(아레나) = 16명, NEXUSBLITZ(돌격넥서스) = 12명, 나머지 = 10명
    private int getExpectedParticipantCount(Match match) {
        String gameMode = match.getGameMode();
        if (gameMode == null) return 10;
        return switch (gameMode) {
            case "CHERRY"     -> 18;
            case "NEXUSBLITZ" -> 12;
            default           -> 10;
        };
    }

    // 조회 전용 Riot API 호출 없음

    // DB에서 페이지 단위로 매치목록 반환
    @Transactional(readOnly = true)
    public Page<MatchRecordDto> getMatchRecordsByPuuid(String puuid, Pageable pageable) {
        return participantRepository.findMatchRecordByPuuid(puuid, pageable);
    }

    // 해당 판 전체 참가자 상세 정보 반환
    public List<MatchSummaryDto> getParticipantSummaryListByMatchId(String matchId) {
        List<Participant> participantList = participantRepository.findByMatchIdForParticipantList(matchId);
        Match match = matchRepository.findByMatchId(matchId);

        if (match == null) {
            throw new IllegalStateException("Match not found: " + matchId);
        }

        List<MatchSummaryDto> matchSummaryDtoList = new ArrayList<>();
        for (Participant participant : participantList) {
            matchSummaryDtoList.add(MatchSummaryDto.from(match, participant));
        }
        return matchSummaryDtoList;
    }


    // 갱신 전용 - 전적갱신 버튼 클릭 시에만 호출

    // Riot API에서 최근 20개 받아서 DB에 없는 것만 저장, 새로 저장된 수 반환
    public int refreshMatchesByPuuid(String puuid) {
        List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);

        int newCount = 0;
        for (String matchId : matchIdList) {
            Match existing = matchRepository.findByMatchId(matchId);
            if (existing == null) {
                processMatch(puuid, matchId);
                newCount++;
            }
        }

        return newCount;
    }

    private MatchRecordDto processMatch(String puuid, String riotMatchId) {
        String targetMatchId = riotMatchId;
        Match match = matchRepository.findByMatchId(targetMatchId);
        List<Participant> matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);

        if (match == null || matchParticipantList.isEmpty()) {
            RiotMatchResponse res = riotMatchClient.getMatchById(targetMatchId);
            targetMatchId = res.getMetadata().getMatchId();

            match = matchRepository.findByMatchId(targetMatchId);
            if (match == null) {
                match = matchRepository.save(Match.from(res));
            }

            int expected = getExpectedParticipantCount(match);

            // DB에 저장된 participantId 목록을 Set으로 관리 (1차 캐시 문제 회피)
            matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);
            Set<Integer> savedParticipantIds = matchParticipantList.stream()
                    .map(Participant::getParticipantId)
                    .collect(Collectors.toSet());

            if (matchParticipantList.size() < expected) {
                for (RiotParticipantResponse rp : res.getInfo().getParticipants()) {
                    if (!savedParticipantIds.contains(rp.getParticipantId())) {
                        participantRepository.save(Participant.from(rp, match));
                        savedParticipantIds.add(rp.getParticipantId()); // 저장 즉시 set에 추가
                    }
                }
            }

            // flush 후 재조회해서 정확한 count 확인
            participantRepository.flush();
            matchParticipantList = participantRepository.findByMatchIdForParticipantList(targetMatchId);

            if (matchParticipantList.size() != expected) {
                throw new IllegalStateException(
                    "Participant count != " + expected + ". matchId=" + targetMatchId
                    + " (actual=" + matchParticipantList.size() + ", gameMode=" + match.getGameMode() + ")");
            }
        }

        int expected = getExpectedParticipantCount(match);
        if (matchParticipantList.size() != expected) {
            throw new IllegalStateException(
                "Participant count != " + expected + ". matchId=" + targetMatchId
                + " (actual=" + matchParticipantList.size() + ", gameMode=" + match.getGameMode() + ")");
        }

        Participant me = null;
        List<MatchRecordDto.ParticipantChampionIcon> icons = new ArrayList<>();
        for (Participant p : matchParticipantList) {
            icons.add(new MatchRecordDto.ParticipantChampionIcon(
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
            throw new IllegalStateException(
                "Requested puuid not found in match participants. matchId=" + targetMatchId);
        }

        participantService.linkSummonerToParticipant(me);
        return MatchRecordDto.from(match, me, icons);
    }


}
