package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock ParticipantService participantService;
    @Mock MatchSaveHelper matchSaveHelper;
    @Mock RiotMatchClient riotMatchClient;
    @Mock LeagueService leagueService;
    @Mock SummonerCrawlService summonerCrawlService;

    MatchService matchService;

    // 매치 수집 병렬 풀 대신 호출 스레드에서 바로 실행하는 Executor 를 주입해 테스트를 결정적으로 만든다.
    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository, participantService, matchSaveHelper, riotMatchClient,
                leagueService, summonerCrawlService, Runnable::run);
    }

    @Test
    @DisplayName("전적 목록 조회는 ParticipantService에 위임해 페이지를 그대로 반환한다")
    void getMatchRecordsByPuuid_delegates() {
        String puuid = "puuid-1";
        Pageable pageable = PageRequest.of(0, 20);
        Page<MatchRecordDto> page = new PageImpl<>(List.of());
        when(participantService.findMatchRecordByPuuid(puuid, pageable)).thenReturn(page);

        Page<MatchRecordDto> result = matchService.getMatchRecordsByPuuid(puuid, pageable);

        assertSame(page, result);
        verify(participantService).findMatchRecordByPuuid(puuid, pageable);
    }

    @Test
    @DisplayName("매치 상세(참가자 요약) 조회는 ParticipantService에 위임한다")
    void getParticipantSummaryList_delegates() {
        String matchId = "KR_1";
        List<MatchSummaryDto> list = List.of();
        when(participantService.findParticipantSummaryListByMatchId(matchId)).thenReturn(list);

        List<MatchSummaryDto> result = matchService.getParticipantSummaryListByMatchId(matchId);

        assertSame(list, result);
        verify(participantService).findParticipantSummaryListByMatchId(matchId);
    }

    @Test
    @DisplayName("DB에 없는 새 매치만 저장하고 저장 수를 반환하며 랭크를 갱신한다")
    void refresh_newMatches_savesAndUpdatesLeague() {
        String puuid = "puuid-1";
        List<String> matchIds = List.of("KR_1", "KR_2");
        when(riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20)).thenReturn(matchIds);
        when(matchRepository.findExistingMatchIds(matchIds)).thenReturn(Set.of("KR_1")); // KR_1은 이미 존재

        int result = matchService.refreshMatchesByPuuid(puuid, RefreshProgress.NONE);

        assertEquals(1, result); // KR_2만 신규
        verify(matchSaveHelper).saveMatchWithParticipants("KR_2", puuid);
        verify(matchSaveHelper, never()).saveMatchWithParticipants("KR_1", puuid);
        verify(leagueService).updateAndSaveLeague(puuid);
    }

    @Test
    @DisplayName("진행률 콜백으로 신규 매치 수(total)와 처리 건수(done)를 보고한다")
    void refresh_reportsProgress() {
        String puuid = "puuid-1";
        List<String> matchIds = List.of("KR_1", "KR_2", "KR_3");
        when(riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20)).thenReturn(matchIds);
        when(matchRepository.findExistingMatchIds(matchIds)).thenReturn(Set.of("KR_1")); // 신규는 KR_2, KR_3

        int[] total = {-1};
        int[] doneCount = {0};
        RefreshProgress progress = new RefreshProgress() {
            @Override public void onTotal(int t) { total[0] = t; }
            @Override public void onMatchDone() { doneCount[0]++; }
        };

        matchService.refreshMatchesByPuuid(puuid, progress);

        assertEquals(2, total[0]);     // 신규 2건이 분모
        assertEquals(2, doneCount[0]); // 2건 모두 처리(전진)
    }
}
