package com.recordsite.backend.controller;

import com.recordsite.backend.dto.RankingPageDto;
import com.recordsite.backend.dto.RankingRowDto;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private static final int MAX_PAGE_SIZE = 100;

    private final RankingService rankingService;

    // 상위 티어 사다리 랭킹 조회. queueType 기본 SOLO. 페이지 size 는 상한을 둔다.
    @GetMapping
    public ResponseEntity<RankingPageDto> getRanking(
            @RequestParam(defaultValue = "SOLO") QueueType queueType,
            @PageableDefault(size = 50) Pageable pageable) {

        int cappedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), cappedSize);

        Page<RankingRowDto> page = rankingService.getRanking(queueType, capped);
        return ResponseEntity.ok(RankingPageDto.from(page));
    }
}
