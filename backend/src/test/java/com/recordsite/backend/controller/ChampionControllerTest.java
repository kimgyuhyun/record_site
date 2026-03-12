package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionSummaryDto;
import com.recordsite.backend.service.ChampionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;


import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@ExtendWith(SpringExtension.class)
@WebMvcTest(ChampionController.class)
class ChampionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChampionService championService;


    @Test
    void getChampionByName() throws Exception {
        String korName = "아리";
        ChampionSummaryDto ahri = new ChampionSummaryDto();
        ahri.setChampionId("Ahri");
        ahri.setChampionKey(1);
        ahri.setNameKor("아리");
        ahri.setNameEn("Ahri");
        ahri.setImageUrl("ahri.jpg");
        when(championService.getChampionByName(korName)).thenReturn(ahri);

        mockMvc.perform(get("/api/champions")
                .param("name", "아리")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.championId").value("Ahri"))
                .andExpect(jsonPath("$.championKey").value(1))
                .andExpect(jsonPath("$.nameKor").value("아리"))
                .andExpect(jsonPath("$.nameEn").value("Ahri"))
                .andExpect(jsonPath("$.imageUrl").value("ahri.jpg"));
    }


    @Test
    void getChampions() throws Exception {
        ChampionSummaryDto c1 = new ChampionSummaryDto();
        ChampionSummaryDto c2 = new ChampionSummaryDto();
        ChampionSummaryDto c3 = new ChampionSummaryDto();
        c1.setChampionId("아리");
        c2.setChampionId("아트록스");
        c3.setChampionId("탈론");

        List<ChampionSummaryDto> champions = List.of(new ChampionSummaryDto[]{c1, c2, c3});

        when(championService.getChampionSummaries()).thenReturn(champions);

        mockMvc.perform(get("/api/champions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].championId").value("아리"))
                .andExpect(jsonPath("$[1].championId").value("아트록스"))
                .andExpect(jsonPath("$[2].championId").value("탈론"));


    }


}