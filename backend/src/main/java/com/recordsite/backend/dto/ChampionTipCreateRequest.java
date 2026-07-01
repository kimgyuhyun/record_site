package com.recordsite.backend.dto;

// 챔피언 팁 작성 요청. 검증(빈값/길이)은 서비스에서 수행한다.
public record ChampionTipCreateRequest(
        int championId,
        String nickname,
        String content
) {
}
