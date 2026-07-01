package com.recordsite.backend.dto;

import java.util.List;

// 챔피언 팁 목록 한 페이지. totalCount 는 헤더의 "(N개)" 표기용, hasNext 는 "더 보기" 노출용.
public record ChampionTipPageResponse(
        List<ChampionTipResponse> tips,
        long totalCount,
        boolean hasNext
) {
}
