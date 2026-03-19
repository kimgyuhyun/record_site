package com.recordsite.backend.controller;

import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.exception.SummonerNotFoundException;
import com.recordsite.backend.service.SummonerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@WebMvcTest(SummonerController.class)
class SummonerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummonerService summonerService;

    @Test
    void getSummonerByRiotIdTest() throws Exception{
        String searchName = "ahri";
        String tagLine = "KR1";
        SummonerDto summonerDto = new SummonerDto();
        summonerDto.setSummonerId("1");
        summonerDto.setName("ahri");
        summonerDto.setPuuid("1513");
        summonerDto.setLevel(300);
        summonerDto.setProfileIconId(38);

        when(summonerService.findSummonerByRiotId(searchName, tagLine))
                .thenReturn(summonerDto);

        mockMvc.perform(get("/api/summoners")
                        .param("name", "ahri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summonerId").value("1"))
                .andExpect(jsonPath("$.name").value("ahri"))
                .andExpect(jsonPath("$.puuid").value("1513"))
                .andExpect(jsonPath("$.level").value(300))
                .andExpect(jsonPath("$.profileIconId").value(38));

    }

    @Test
    void getSummonerByRiotId_notFound_Test() throws Exception {

        when(summonerService.findSummonerByRiotId("null", "KR1"))
                .thenThrow(new SummonerNotFoundException("null"));

        mockMvc.perform(get("/api/summoners")
                        .param("name", "null")
                        .param("tagLine", "KR1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummonerListByNameTest() throws Exception {
        String name = "hide on bush";

        SummonerDto summonerDto1 = new SummonerDto();
        SummonerDto summonerDto2 = new SummonerDto();
        summonerDto1.setName("hide on bush");
        summonerDto2.setName("hide on bush");
        
        when(summonerService.findSummonerListByName(name))
                .thenReturn(List.of(summonerDto1, summonerDto2));

        mockMvc.perform(get("/api/summoners/search")
                        .param("name", "hide on bush"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("hide on bush"))
                .andExpect(jsonPath("$[1].name").value("hide on bush"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSummonerListByName_notFound_Test() throws Exception {

        when(summonerService.findSummonerListByName("null"))
                .thenThrow(new SummonerNotFoundException("null"));

        mockMvc.perform(get("/api/summoners/search")
                        .param("name", "null"))
                .andExpect(status().isNotFound());
    }

}