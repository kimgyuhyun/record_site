package com.recordsite.backend.controller;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.dto.MatchTimelineDto;
import com.recordsite.backend.dto.RefreshJobDto;
import com.recordsite.backend.service.MatchService;
import com.recordsite.backend.service.MatchTimelineService;
import com.recordsite.backend.service.RefreshJobStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    // 페이지 크기 상한. 클라이언트가 size 를 크게 보내도 여기서 잘라 한 번에 퍼가는 양을 서버가 통제한다.
    private static final int MAX_PAGE_SIZE = 50;

    private final MatchService matchService;
    private final MatchTimelineService matchTimelineService;
    private final RefreshJobStore refreshJobStore;

    @GetMapping(params = "puuid")
    public ResponseEntity<Page<MatchRecordDto>> getMatchList(
            @RequestParam String puuid,
            @PageableDefault(size = 20) Pageable pageable) throws Exception {
        int cappedSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());

        Page<MatchRecordDto> matchRecordDto = matchService.getMatchRecordsByPuuid(puuid, capped);
        return ResponseEntity.ok(matchRecordDto);
    }

    @GetMapping("/{matchId}/summary")
    public ResponseEntity<List<MatchSummaryDto>> getMatchSummaryList(@PathVariable String matchId) throws Exception {
        return ResponseEntity.ok(matchService.getParticipantSummaryListByMatchId(matchId));
    }

    // 전적 갱신 요청을 작업 큐에 넣고 즉시 jobId 를 응답한다(동기 수집 X).
    // 이미 갱신 중(쿨다운 포함)이면 새 작업 없이 기존 작업 상태를 그대로 돌려준다.
    @PostMapping("/refresh")
    public ResponseEntity<RefreshJobDto> refreshMatches(@RequestParam String puuid) {
        return ResponseEntity.ok(refreshJobStore.submit(puuid));
    }

    // 프론트가 2~3초 간격으로 폴링하는 진행 상황 조회. 잡이 만료/없으면 404.
    @GetMapping("/refresh-jobs/{jobId}")
    public ResponseEntity<RefreshJobDto> getRefreshJob(@PathVariable String jobId) {
        RefreshJobDto job = refreshJobStore.find(jobId);
        return job == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(job);
    }

    // 타임라인 화면(맵/이벤트/골드 그래프)용 — 열 때 즉석으로 Riot 타임라인을 받아 가공해 반환
    @GetMapping("/{matchId}/timeline")
    public ResponseEntity<MatchTimelineDto> getMatchTimeline(@PathVariable String matchId) {
        return ResponseEntity.ok(matchTimelineService.getTimeline(matchId));
    }
}
