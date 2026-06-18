package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.service.MatchService;
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
    @DisplayName("POST /api/matches/refresh 는 새로 저장된 매치 수를 반환한다")
    void refreshMatches() throws Exception {
        when(matchService.refreshMatchesByPuuid("puuid-1")).thenReturn(3);

        mockMvc.perform(post("/api/matches/refresh").param("puuid", "puuid-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
