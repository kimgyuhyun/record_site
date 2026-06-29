package com.recordsite.backend.service;

import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.entity.RankSnapshot;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.RankSnapshotRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// 판당 LP 증감 계산(getLpChangesByPuuid) 검증.
// 스냅샷은 "그 매치 직후의 LP" 를 뜻하므로, 두 스냅샷 차이가 곧 그 사이 LP 변동이다.
// 단, 사이에 낀 랭크 게임이 정확히 1판이고(귀속 가능) 부호가 승패와 맞을 때만 그 매치에 증감을 표시한다.
@ExtendWith(MockitoExtension.class)
class RankSnapshotServiceTest {

    private static final String PUUID = "puuid-1";

    @Mock RankSnapshotRepository rankSnapshotRepository;
    @Mock ParticipantRepository participantRepository;
    @Mock SummonerRepository summonerRepository;

    @InjectMocks RankSnapshotService rankSnapshotService;

    // GOLD IV 기준 ladderScore = 1200 + 0 + lp. of(...) 가 내부에서 환산한다.
    private RankSnapshot soloSnapshot(int leaguePoints, String anchorMatchId) {
        return RankSnapshot.of(PUUID, QueueType.SOLO, "GOLD", "IV", leaguePoints, anchorMatchId);
    }

    private void anchorPlayedAt(String matchId, long gameCreation) {
        when(participantRepository.findGameCreationByPuuidAndMatchId(PUUID, matchId))
                .thenReturn(gameCreation);
    }

    private void rankedGamesBetweenAnchors(long count) {
        when(participantRepository.countRankedMatchesInWindow(
                eq(PUUID), anyInt(), anyLong(), anyLong())).thenReturn(count);
    }

    @Test
    @DisplayName("앵커가 한 판으로 끊긴 승리: LP 차이를 +로 그 매치에 귀속한다")
    void isolatedWin_showsPositiveLp() {
        when(rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(PUUID))
                .thenReturn(List.of(soloSnapshot(50, "M0"), soloSnapshot(70, "M1")));
        anchorPlayedAt("M0", 1000L);
        anchorPlayedAt("M1", 2000L);
        rankedGamesBetweenAnchors(1);
        when(participantRepository.findWinByPuuidAndMatchId(PUUID, "M1")).thenReturn(true);

        Map<String, Integer> result = rankSnapshotService.getLpChangesByPuuid(PUUID);

        assertEquals(20, result.get("M1"));   // 1270 - 1250
        assertFalse(result.containsKey("M0")); // 첫 스냅샷은 비교 대상이 없어 미표시
    }

    @Test
    @DisplayName("앵커가 한 판으로 끊긴 패배: LP 차이를 -로 그 매치에 귀속한다")
    void isolatedLoss_showsNegativeLp() {
        when(rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(PUUID))
                .thenReturn(List.of(soloSnapshot(50, "M0"), soloSnapshot(32, "M1")));
        anchorPlayedAt("M0", 1000L);
        anchorPlayedAt("M1", 2000L);
        rankedGamesBetweenAnchors(1);
        when(participantRepository.findWinByPuuidAndMatchId(PUUID, "M1")).thenReturn(false);

        Map<String, Integer> result = rankSnapshotService.getLpChangesByPuuid(PUUID);

        assertEquals(-18, result.get("M1"));  // 1232 - 1250
    }

    @Test
    @DisplayName("패배인데 LP가 +로 계산되면(부호 불일치) 오귀속이므로 표시하지 않는다 — '패배 +53' 버그")
    void lossWithPositiveDelta_isDropped() {
        // 사이에 여러 판을 했지만 DB엔 마지막 패배만 남아 count=1 로 보이는 상황을 가정.
        when(rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(PUUID))
                .thenReturn(List.of(soloSnapshot(20, "M0"), soloSnapshot(73, "M1")));
        anchorPlayedAt("M0", 1000L);
        anchorPlayedAt("M1", 2000L);
        rankedGamesBetweenAnchors(1);
        when(participantRepository.findWinByPuuidAndMatchId(PUUID, "M1")).thenReturn(false); // 패배

        Map<String, Integer> result = rankSnapshotService.getLpChangesByPuuid(PUUID);

        assertFalse(result.containsKey("M1")); // +53 이지만 패배라 버려짐
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("두 갱신 사이에 여러 판을 했으면(귀속 불가) 표시하지 않는다")
    void multipleGamesBetweenAnchors_isDropped() {
        when(rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(PUUID))
                .thenReturn(List.of(soloSnapshot(50, "M0"), soloSnapshot(90, "M3")));
        anchorPlayedAt("M0", 1000L);
        anchorPlayedAt("M3", 4000L);
        rankedGamesBetweenAnchors(3); // 사이에 3판
        // count!=1 에서 바로 끊기므로 승패 조회는 호출되지 않는다(불필요 스텁 방지 위해 lenient)
        lenient().when(participantRepository.findWinByPuuidAndMatchId(PUUID, "M3")).thenReturn(true);

        Map<String, Integer> result = rankSnapshotService.getLpChangesByPuuid(PUUID);

        assertTrue(result.isEmpty());
    }
}
