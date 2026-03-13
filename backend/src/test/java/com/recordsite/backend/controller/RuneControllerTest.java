package com.recordsite.backend.controller;

import com.recordsite.backend.dto.RuneDto;
import com.recordsite.backend.dto.RunePathDto;
import com.recordsite.backend.service.RuneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@WebMvcTest(RuneController.class)
class RuneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RuneService runeService;


    @Test
    void getRunePathListTest() throws Exception {
        RunePathDto rp1 = new RunePathDto();
        RunePathDto rp2 = new RunePathDto();
        rp1.setRunePathNameKor("정밀");
        rp2.setRunePathNameKor("지배");

        when(runeService.findAllRunePathList()).thenReturn(List.of(rp1,rp2));

        mockMvc.perform(get("/api/runePaths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runePathNameKor").value("정밀"))
                .andExpect(jsonPath("$[1].runePathNameKor").value("지배"))
                .andExpect(jsonPath("$.length()").value(2));

    }

    @Test
    void getRuneListTest() throws Exception {

        RuneDto r1 = new RuneDto();
        RuneDto r2 = new RuneDto();
        r1.setRuneNameKor("정복자");
        r1.setRuneKey(11);;
        r1.setPathKey(1);
        r2.setRuneNameKor("감전");
        r2.setPathKey(2);
        r2.setRuneKey(22);

        when(runeService.findAllRuneList()).thenReturn(List.of(r1,r2));

        mockMvc.perform(get("/api/runePaths/runes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runeNameKor").value("정복자"))
                .andExpect(jsonPath("$[0].runeKey").value(11))
                .andExpect(jsonPath("$[0].pathKey").value(1))
                .andExpect(jsonPath("$[1].runeNameKor").value("감전"))
                .andExpect(jsonPath("$[1].runeKey").value(22))
                .andExpect(jsonPath("$[1].pathKey").value(2))
                .andExpect(jsonPath("$.length()").value(2));
    }

}