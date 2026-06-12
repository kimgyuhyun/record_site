package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final MatchSaveHelper matchSaveHelper;
    private final RiotMatchClient riotMatchClient;


    // ──────────────────────────────────────────
    // 조회 전용 - Riot API 호출 없음
    // ──────────────────────────────────────────

    // DB에서 페이지 단위로 전적 목록 반환
    @Transactional(readOnly = true)
    public Page<MatchRecordDto> getMatchRecordsByPuuid(String puuid, Pageable pageable) {
        return participantRepository.findMatchRecordByPuuid(puuid, pageable);
    }

    // 해당 판 전체 참가자 상세 정보 반환
    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getParticipantSummaryListByMatchId(String matchId) {
        Match match = matchRepository.findByMatchId(matchId);
        if (match == null) {
            throw new IllegalStateException("Match not found: " + matchId);
        }
        List<Participant> participantList = participantRepository.findByMatchIdForParticipantList(matchId);
        List<MatchSummaryDto> result = new ArrayList<>();
        for (Participant participant : participantList) {
            result.add(MatchSummaryDto.from(match, participant));
        }
        return result;
    }

    // ──────────────────────────────────────────
    // 갱신 전용 - 전적갱신 버튼 클릭 시에만 호출
    // ──────────────────────────────────────────

    // Riot API에서 최근 20개 받아서 DB에 없는 것만 저장, 새로 저장된 수 반환
    @Transactional(readOnly = true)
    public int refreshMatchesByPuuid(String puuid) {
        List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);

        int newCount = 0;
        for (String matchId : matchIdList) {
            if (matchRepository.findByMatchId(matchId) == null) {
                try {
                    matchSaveHelper.saveMatchWithParticipants(matchId, puuid);
                    newCount++;
                } catch (Exception e) {
                    log.warn("매치 저장 실패, 스킵: matchId={}, error={}", matchId, e.getMessage());
                }
            }
        }
        return newCount;
    }
}
