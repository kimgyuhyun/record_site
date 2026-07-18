package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ChampionTipCreateRequest;
import com.recordsite.backend.dto.ChampionTipPageResponse;
import com.recordsite.backend.dto.ChampionTipPasswordRequest;
import com.recordsite.backend.dto.ChampionTipResponse;
import com.recordsite.backend.dto.ChampionTipUpdateRequest;
import com.recordsite.backend.service.ChampionTipService;
import com.recordsite.backend.support.TipActorKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 챔피언 운영 팁(코멘트) API. 챔피언 상세 페이지 하단 팁 게시판에서 사용한다.
@RestController
@RequestMapping("/api/champion-tips")
@RequiredArgsConstructor
public class ChampionTipController {

    private final ChampionTipService championTipService;
    private final TipActorKeyResolver actorKeyResolver;

    // 특정 챔피언의 팁 목록. sort=popular(기본)|recent, language/patchVersion 지정 시 해당 값만 필터.
    @GetMapping
    public ResponseEntity<ChampionTipPageResponse> getTips(
            @RequestParam int championId,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String patchVersion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(championTipService.getTips(championId, sort, language, patchVersion, page, size));
    }

    // 팁 작성. 본문 { championId, nickname, content }
    @PostMapping
    public ResponseEntity<ChampionTipResponse> createTip(@RequestBody ChampionTipCreateRequest request) {
        return ResponseEntity.ok(championTipService.createTip(request));
    }

    // 추천/비추천. direction=UP|DOWN. 비로그인이라 요청자 식별은 IP 해시(actorKey)로 한다 — 팁당 1회.
    @PostMapping("/{tipId}/vote")
    public ResponseEntity<Void> vote(@PathVariable Long tipId, @RequestParam String direction,
                                     HttpServletRequest request) {
        championTipService.vote(tipId, direction, actorKeyResolver.resolve(request));
        return ResponseEntity.ok().build();
    }

    // 신고(누적 시 자동 숨김). 팁당 1회 — 이게 없으면 혼자서 임의의 글을 숨길 수 있다.
    @PostMapping("/{tipId}/report")
    public ResponseEntity<Void> report(@PathVariable Long tipId, HttpServletRequest request) {
        championTipService.report(tipId, actorKeyResolver.resolve(request));
        return ResponseEntity.ok().build();
    }

    // 수정 — 비밀번호 일치 시 내용 변경. 본문 { password, content }
    @PutMapping("/{tipId}")
    public ResponseEntity<ChampionTipResponse> update(@PathVariable Long tipId,
                                                      @RequestBody ChampionTipUpdateRequest request) {
        return ResponseEntity.ok(championTipService.updateTip(tipId, request));
    }

    // 삭제 — 작성 시 정한 비밀번호가 일치해야 한다. 본문 { password }
    @DeleteMapping("/{tipId}")
    public ResponseEntity<Void> delete(@PathVariable Long tipId,
                                       @RequestBody ChampionTipPasswordRequest request) {
        championTipService.deleteTip(tipId, request.password());
        return ResponseEntity.ok().build();
    }
}
