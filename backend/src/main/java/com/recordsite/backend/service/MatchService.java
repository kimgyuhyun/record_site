package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.SummonerDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final ParticipantService participantService;
    private final MatchSaveHelper matchSaveHelper;
    private final RiotMatchClient riotMatchClient;
    private final LeagueService leagueService;
    private final SummonerService summonerService;


    // ──────────────────────────────────────────
    // 조회 전용 - Riot API 호출 없음
    // ──────────────────────────────────────────

    // DB에서 페이지 단위로 전적 목록 반환
    @Transactional(readOnly = true)
    public Page<MatchRecordDto> getMatchRecordsByPuuid(String puuid, Pageable pageable) {
        return participantService.findMatchRecordByPuuid(puuid, pageable);
    }

    // 해당 판 전체 참가자 상세 정보 반환
    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getParticipantSummaryListByMatchId(String matchId) {
        return participantService.findParticipantSummaryListByMatchId(matchId);

    }

    // ──────────────────────────────────────────
    // 갱신 전용 - 전적갱신 버튼 클릭 시에만 호출
    // ──────────────────────────────────────────

    // Riot API에서 최근 전적 20개 받아서 DB에 없는 것만 저장,
    // 자유/솔로 랭크도 갱신 
    // 새로 저장된 수 반환
    public int refreshMatchesByPuuid(String puuid) {

        // 갱신 체크
        SummonerDto summoner = summonerService.findByPuuid(puuid);
        LocalDateTime rankUpdatedAt = summoner.getRankUpdatedAt();
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);
        boolean isUpdateRecently = rankUpdatedAt != null &&
                rankUpdatedAt.isAfter(threeMinutesAgo);
        if (isUpdateRecently) {
            return -1;
        }

        // 라이엇 API 에서 최근 매치 20개 조회
        List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
        if (matchIdList.isEmpty()) return 0;

        // DB에 이미 있는 matchId를 한 번의 쿼리로 조회 -> Set 으로 변환
        Set<String> existingIds = matchRepository.findExistingMatchIds(matchIdList);

        List<String> newMatchIds = matchIdList.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (newMatchIds.isEmpty()) return 0;

        // db에 없는 매치 저장
        int newCount = 0;
        for (String matchId : newMatchIds) {
                try {
                    matchSaveHelper.saveMatchWithParticipants(matchId, puuid);
                    newCount++;
                } catch (Exception e) {
                    log.warn("매치 저장 실패, 스킵: matchId={}, error={}", matchId, e.getMessage());
                }

        }

        // 랭크 갱신 + 최근 갱신시간 찍기
        leagueService.updateAndSaveLeague(puuid);

        return newCount;
    }
}
