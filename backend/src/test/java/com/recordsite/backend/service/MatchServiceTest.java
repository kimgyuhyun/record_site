package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.repository.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock ParticipantService participantService;
    @Mock MatchSaveHelper matchSaveHelper;
    @Mock RiotMatchClient riotMatchClient;
    @Mock LeagueService leagueService;
    @Mock SummonerService summonerService;
    @Mock RankSnapshotService rankSnapshotService;
    @Mock SummonerCrawlService summonerCrawlService;

    @InjectMocks MatchService matchService;

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
    @DisplayName("3분 이내 갱신 이력이 있으면 -1을 반환하고 Riot 호출을 하지 않는다")
    void refresh_recentlyUpdated_returnsMinusOne() {
        String puuid = "puuid-1";
        SummonerDto dto = new SummonerDto();
        dto.setRankUpdatedAt(LocalDateTime.now().minusMinutes(1));
        when(summonerService.findByPuuid(puuid)).thenReturn(dto);

        int result = matchService.refreshMatchesByPuuid(puuid);

        assertEquals(-1, result);
        verifyNoInteractions(riotMatchClient);
        verify(matchSaveHelper, never()).saveMatchWithParticipants(anyString(), anyString());
    }

    @Test
    @DisplayName("DB에 없는 새 매치만 저장하고 저장 수를 반환하며 랭크를 갱신한다")
    void refresh_newMatches_savesAndUpdatesLeague() {
        String puuid = "puuid-1";
        SummonerDto dto = new SummonerDto();
        dto.setRankUpdatedAt(LocalDateTime.now().minusMinutes(10)); // 갱신 가능
        when(summonerService.findByPuuid(puuid)).thenReturn(dto);

        List<String> matchIds = List.of("KR_1", "KR_2");
        when(riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20)).thenReturn(matchIds);
        when(matchRepository.findExistingMatchIds(matchIds)).thenReturn(Set.of("KR_1")); // KR_1은 이미 존재

        int result = matchService.refreshMatchesByPuuid(puuid);

        assertEquals(1, result); // KR_2만 신규
        verify(matchSaveHelper).saveMatchWithParticipants("KR_2", puuid);
        verify(matchSaveHelper, never()).saveMatchWithParticipants("KR_1", puuid);
        verify(leagueService).updateAndSaveLeague(puuid);
    }
}
