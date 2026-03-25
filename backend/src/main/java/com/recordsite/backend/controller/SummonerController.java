package com.recordsite.backend.controller;

import com.recordsite.backend.dto.SummonerDto;
import com.recordsite.backend.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/summoners")
@RequiredArgsConstructor
public class SummonerController {
    private final SummonerService summonerService;

    @GetMapping()
    public ResponseEntity<SummonerDto> getSummonerByName(
            @RequestParam String name,
            @RequestParam(required = false) String tagLine, // 유저가 입력한 태그값
            @RequestParam(required = false, defaultValue = "KR") String region // 지역 선택값
    ) throws Exception {
        String resolvedTagLine = (tagLine != null && !tagLine.isBlank())
                ? tagLine.trim().toUpperCase().replaceFirst("^#", "") // 태그가 있으면 대문자로 변경해서 넘김
                : deFaultTagLineByRegion(region); // 태그가 없으면 리전을 태그로 변환해서 넘김

        return ResponseEntity.ok(summonerService.findSummonerByRiotId(name, resolvedTagLine));
    }

    private String deFaultTagLineByRegion(String region) {
        if (region == null) return "KR1";

        return switch (region.trim().toUpperCase()) {
            case "KR" -> "KR1";
            case "NA" -> "NA1";
            case "EUW" -> "EUW1";
            case "EUNE" -> "EUN1";
            case "JP" -> "JP1";
            case "BR" -> "BR1";
            case "LAN" -> "LA1";
            case "LAS" -> "LA2";
            case "OCE" -> "OC1";
            case "TR" -> "TR1";
            case "RU" -> "RU1";
            default -> "KR1";
        };
    }

    @GetMapping("/search")
    public ResponseEntity<List<SummonerDto>> getSummonerListByName(
            @RequestParam String name) throws Exception {
        return ResponseEntity.ok(summonerService.findSummonerListByName(name));
    }


}
