package com.recordsite.backend.dto;

import com.recordsite.backend.entity.ChampionTip;

import java.time.LocalDateTime;

// 챔피언 팁 한 건 응답. score = 추천 - 비추천(프론트 정렬/표시용으로 미리 계산해 내려준다).
public record ChampionTipResponse(
        Long id,
        String nickname,
        String content,
        String patchVersion,
        String language,
        int upvotes,
        int downvotes,
        int score,
        LocalDateTime createdAt
) {
    public static ChampionTipResponse from(ChampionTip tip) {
        return new ChampionTipResponse(
                tip.getId(), tip.getNickname(), tip.getContent(),
                tip.getPatchVersion(), tip.getLanguage(),
                tip.getUpvotes(), tip.getDownvotes(), tip.score(), tip.getCreatedAt());
    }
}
