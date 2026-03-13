package com.recordsite.backend.controller;

import com.recordsite.backend.dto.RuneDto;
import com.recordsite.backend.dto.RunePathDto;
import com.recordsite.backend.service.RuneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runePaths")
@RequiredArgsConstructor
public class RuneController {

    private final RuneService runeService;

    @GetMapping
    public List<RunePathDto> getRunePathList() throws Exception {
        return runeService.findAllRunePathList();
    }


    @GetMapping("/runes")
    public List<RuneDto> getRuneList() throws Exception {
        return runeService.findAllRuneList();
    }
}
