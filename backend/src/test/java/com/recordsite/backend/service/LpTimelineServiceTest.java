package com.recordsite.backend.service;

import com.recordsite.backend.entity.LpReading;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.LpReadingRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.RankedGameView;
import com.recordsite.backend.repository.SummonerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// 판당 LP 증감 계산(getLpChangesByPuuid) 검증.
// LP 측정값은 시간순 타임라인으로 쌓이고, 인접한 두 측정값이 정확히 한 판의 랭크 게임만 사이에 둘 때
// 그 차이(ladderScore 차)를 그 판에 귀속한다. 부호가 승패와 맞을 때만 표시한다.
@ExtendWith(MockitoExtension.class)
class LpTimelineServiceTest {

    private static final String PUUID = "puuid-1";
    private static final int SOLO_QUEUE_ID = QueueType.SOLO.queueId();

    @Mock LpReadingRepository lpReadingRepository;
    @Mock ParticipantRepository participantRepository;
    @Mock SummonerRepository summonerRepository;

    @InjectMocks LpTimelineService lpTimelineService;

    // GOLD IV 기준 ladderScore = 1200 + lp. of(...) 가 내부에서 환산한다.
    private LpReading soloReading(int leaguePoints, long readAtEpochMs) {
        return LpReading.of(PUUID, QueueType.SOLO, "GOLD", "IV", leaguePoints, readAtEpochMs);
    }

    private record Game(String matchId, long gameCreation, boolean win) implements RankedGameView {
        @Override public String getMatchId() { return matchId; }
        @Override public Long getGameCreation() { return gameCreation; }
        @Override public Boolean getWin() { return win; }
    }

    private void soloReadings(LpReading... readings) {
        when(lpReadingRepository.findByPuuidOrderByReadAtEpochMsAsc(PUUID)).thenReturn(List.of(readings));
    }

    private void soloRankedGames(Game... games) {
        when(participantRepository.findRankedGamesByPuuidAndQueueId(PUUID, SOLO_QUEUE_ID))
                .thenReturn(List.of(games));
    }

    @Test
    @DisplayName("한 판으로 끊긴 승리: 두 측정값 차이를 +로 그 매치에 귀속한다")
    void isolatedWin_showsPositiveLp() {
        soloReadings(soloReading(50, 1000L), soloReading(70, 2000L));
        soloRankedGames(new Game("M1", 1500L, true)); // (1000, 2000] 안에 한 판

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertEquals(20, result.get("M1")); // 1270 - 1250
    }

    @Test
    @DisplayName("한 판으로 끊긴 패배: 두 측정값 차이를 -로 그 매치에 귀속한다")
    void isolatedLoss_showsNegativeLp() {
        soloReadings(soloReading(50, 1000L), soloReading(32, 2000L));
        soloRankedGames(new Game("M1", 1500L, false));

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertEquals(-18, result.get("M1")); // 1232 - 1250
    }

    @Test
    @DisplayName("패배인데 LP가 +로 계산되면(부호 불일치) 오귀속이므로 표시하지 않는다 — '패배 +53' 버그")
    void lossWithPositiveDelta_isDropped() {
        soloReadings(soloReading(20, 1000L), soloReading(73, 2000L));
        soloRankedGames(new Game("M1", 1500L, false)); // 패배인데 +53

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertFalse(result.containsKey("M1"));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("두 측정값 사이에 여러 판이면(귀속 불가) 표시하지 않는다")
    void multipleGamesInWindow_isDropped() {
        soloReadings(soloReading(50, 1000L), soloReading(90, 4000L));
        soloRankedGames(
                new Game("M1", 1500L, true),
                new Game("M2", 2500L, true),
                new Game("M3", 3500L, true)); // (1000, 4000] 안에 세 판

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("측정값이 촘촘하면 각 단판 구간이 따로 귀속된다(측정값-매치 분리의 핵심 효과)")
    void denseReadings_attributeEachGameSeparately() {
        // 측정값 3개가 게임 2판을 각각 단판 구간으로 끊는다: (1000,2000]=M1, (2000,3000]=M2
        soloReadings(soloReading(50, 1000L), soloReading(70, 2000L), soloReading(58, 3000L));
        soloRankedGames(
                new Game("M1", 1500L, true),   // +20
                new Game("M2", 2500L, false)); // -12

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertEquals(20, result.get("M1"));  // 1270 - 1250
        assertEquals(-12, result.get("M2")); // 1258 - 1270
    }

    @Test
    @DisplayName("측정값 사이에 게임이 없는 구간은 어떤 매치에도 귀속하지 않는다")
    void windowWithNoGame_attributesNothing() {
        soloReadings(soloReading(50, 1000L), soloReading(50, 2000L)); // LP 변동 없는 두 측정
        soloRankedGames(); // 이 큐 랭크 게임 없음

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("측정값이 하나뿐이면 비교할 인접쌍이 없어 표시하지 않는다")
    void singleReading_attributesNothing() {
        soloReadings(soloReading(50, 1000L));

        Map<String, Integer> result = lpTimelineService.getLpChangesByPuuid(PUUID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("쓰기: 직전 측정값과 LP가 같으면 새 측정값을 만들지 않는다")
    void recordReadings_skipsWhenLpUnchanged() {
        com.recordsite.backend.entity.Summoner summoner = org.mockito.Mockito.mock(
                com.recordsite.backend.entity.Summoner.class);
        when(summonerRepository.findBypuuid(PUUID)).thenReturn(summoner);
        when(summoner.getSoloTier()).thenReturn("GOLD");
        when(summoner.getSoloRank()).thenReturn("IV");
        when(summoner.getSoloLp()).thenReturn(50);
        when(summoner.getFlexTier()).thenReturn(null); // 자유랭 언랭 → 측정 없음
        when(lpReadingRepository.findFirstByPuuidAndQueueTypeOrderByReadAtEpochMsDesc(
                eq(PUUID), eq(QueueType.SOLO)))
                .thenReturn(soloReading(50, 999L)); // 직전 측정값도 GOLD IV 50 → 동일

        lpTimelineService.recordReadings(PUUID);

        org.mockito.Mockito.verify(lpReadingRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
