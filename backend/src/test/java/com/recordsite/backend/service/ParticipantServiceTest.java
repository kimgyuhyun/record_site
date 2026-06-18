package com.recordsite.backend.service;

import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.service.ParticipantService.RankedTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantServiceTest {

    private static final int QUEUE_SOLO = 420;
    private static final int QUEUE_FLEX = 440;
    private static final int QUEUE_ARAM = 450;

    private Summoner summonerWithRanks() {
        return Summoner.builder()
                .soloTier("MASTER").soloRank(null) // 마스터+ 는 단계 없음
                .flexTier("GOLD").flexRank("II")
                .build();
    }

    @Test
    @DisplayName("솔로랭크 큐(420)는 솔로 티어/단계를 반환한다")
    void resolveRankedTier_solo() {
        RankedTier tier = ParticipantService.resolveRankedTier(QUEUE_SOLO, summonerWithRanks());

        assertEquals("MASTER", tier.tier());
        assertNull(tier.rank());
    }

    @Test
    @DisplayName("자유랭크 큐(440)는 자유 티어/단계를 반환한다")
    void resolveRankedTier_flex() {
        RankedTier tier = ParticipantService.resolveRankedTier(QUEUE_FLEX, summonerWithRanks());

        assertEquals("GOLD", tier.tier());
        assertEquals("II", tier.rank());
    }

    @Test
    @DisplayName("랭크 큐가 아니면(칼바람 등) 빈 티어를 반환한다")
    void resolveRankedTier_nonRanked() {
        RankedTier tier = ParticipantService.resolveRankedTier(QUEUE_ARAM, summonerWithRanks());

        assertNull(tier.tier());
        assertNull(tier.rank());
    }

    @Test
    @DisplayName("소환사 정보가 없으면 빈 티어를 반환한다")
    void resolveRankedTier_nullSummoner() {
        RankedTier tier = ParticipantService.resolveRankedTier(QUEUE_SOLO, null);

        assertNull(tier.tier());
        assertNull(tier.rank());
    }
}
