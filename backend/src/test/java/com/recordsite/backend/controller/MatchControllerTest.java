package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.RefreshJobDto;
import com.recordsite.backend.service.MatchService;
import com.recordsite.backend.service.MatchTimelineService;
import com.recordsite.backend.service.RefreshJobStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private MatchTimelineService matchTimelineService;

    @MockitoBean
    private RefreshJobStore refreshJobStore;

    @Test
    @DisplayName("GET /api/matches/{matchId}/summary 는 참가자 요약 목록(룬 포함)을 반환한다")
    void getMatchSummaryList() throws Exception {
        MatchSummaryDto dto = MatchSummaryDto.builder()
                .matchId("KR_1")
                .championName("Ahri")
                .keystoneId(8112)
                .subStyleId(8000)
                .build();
        when(matchService.getParticipantSummaryListByMatchId("KR_1"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/matches/{matchId}/summary", "KR_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchId").value("KR_1"))
                .andExpect(jsonPath("$[0].championName").value("Ahri"))
                .andExpect(jsonPath("$[0].keystoneId").value(8112))
                .andExpect(jsonPath("$[0].subStyleId").value(8000));
    }

    @Test
    @DisplayName("POST /api/matches/refresh 는 갱신 작업을 큐에 넣고 jobId와 PENDING 상태를 반환한다")
    void refreshMatches() throws Exception {
        when(refreshJobStore.submit("puuid-1"))
                .thenReturn(new RefreshJobDto("job-1", "PENDING", 0, 0));

        mockMvc.perform(post("/api/matches/refresh").param("puuid", "puuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.done").value(0));
    }

    @Test
    @DisplayName("GET /api/matches/refresh-jobs/{jobId} 는 진행 상황을 반환한다")
    void getRefreshJob() throws Exception {
        when(refreshJobStore.find("job-1"))
                .thenReturn(new RefreshJobDto("job-1", "PROCESSING", 20, 7));

        mockMvc.perform(get("/api/matches/refresh-jobs/{jobId}", "job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.done").value(7));
    }
}
