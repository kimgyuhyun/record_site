package com.recordsite.backend.service;

import com.recordsite.backend.entity.LpReading;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.LpReadingRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.RankedGameView;
import com.recordsite.backend.repository.SummonerRepository;
import com.recordsite.backend.domain.LadderScore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 판당 LP 증감(LP 측정값 타임라인 방식)의 쓰기/읽기 조율자.
// op.gg/fow 와 같은 원리: 라이엇 API 엔 판당 LP 가 없으므로, LP 를 자주 측정해 타임라인으로 쌓고
// "한 게임을 사이에 둔 두 측정값의 차이"를 그 게임의 LP 증감으로 본다. 측정값을 매치에 묶지 않는 게 핵심 —
// 매치당 측정값 1개로 고정하던 구 모델(rank_snapshot)은 측정이 드물어 거의 항상 귀속 불가였다.
//  - 쓰기: 갱신/폴링마다 현재 LP 를 측정값으로 한 줄 적재(직전과 LP 가 같으면 생략해 행 폭증 방지)
//  - 읽기: 큐별로 측정값을 시간순으로 보며, 두 인접 측정 사이에 낀 랭크 게임이 정확히 한 판일 때만
//          그 차이를 그 판에 귀속한다(여러 판이 끼면 쪼갤 수 없고, 부호가 승패와 어긋나면 오귀속이라 버린다)
@Service
@RequiredArgsConstructor
public class LpTimelineService {

    private final LpReadingRepository lpReadingRepository;
    private final ParticipantRepository participantRepository;
    private final SummonerRepository summonerRepository;

    @Transactional
    public void recordReadings(String puuid) {
        Summoner summoner = summonerRepository.findBypuuid(puuid);
        if (summoner == null) return;

        long now = System.currentTimeMillis();
        recordForQueue(puuid, QueueType.SOLO,
                summoner.getSoloTier(), summoner.getSoloRank(), summoner.getSoloLp(), now);
        recordForQueue(puuid, QueueType.FLEX,
                summoner.getFlexTier(), summoner.getFlexRank(), summoner.getFlexLp(), now);
    }

    private void recordForQueue(String puuid, QueueType queueType,
                                String tier, String division, Integer leaguePoints, long readAtEpochMs) {
        if (tier == null || leaguePoints == null) return; // 언랭 큐는 측정값 없음

        LpReading latest = lpReadingRepository
                .findFirstByPuuidAndQueueTypeOrderByReadAtEpochMsDesc(puuid, queueType);
        int ladderScore = LadderScore.of(tier, division, leaguePoints);
        if (latest != null && latest.getLadderScore() == ladderScore) return; // 변동 없으면 새 줄 생략

        lpReadingRepository.save(
                LpReading.of(puuid, queueType, tier, division, leaguePoints, readAtEpochMs));
    }

    // 판당 LP 증감 맵(matchId -> ±LP). 큐별로 측정값을 시간순으로 훑으며, 인접한 두 측정값 (이전, 현재) 이
    // 정확히 한 판의 랭크 게임만 사이에 두고 있을 때 그 차이를 그 판에 귀속한다.
    @Transactional(readOnly = true)
    public Map<String, Integer> getLpChangesByPuuid(String puuid) {
        List<LpReading> readings = lpReadingRepository.findByPuuidOrderByReadAtEpochMsAsc(puuid);

        Map<QueueType, List<LpReading>> readingsByQueue = new EnumMap<>(QueueType.class);
        for (LpReading reading : readings) {
            readingsByQueue.computeIfAbsent(reading.getQueueType(), q -> new ArrayList<>()).add(reading);
        }

        Map<String, Integer> lpChangeByMatchId = new HashMap<>();
        for (Map.Entry<QueueType, List<LpReading>> entry : readingsByQueue.entrySet()) {
            attributeForQueue(puuid, entry.getKey(), entry.getValue(), lpChangeByMatchId);
        }
        return lpChangeByMatchId;
    }

    private void attributeForQueue(String puuid, QueueType queueType,
                                   List<LpReading> queueReadings,
                                   Map<String, Integer> lpChangeByMatchId) {
        if (queueReadings.size() < 2) return; // 비교할 인접쌍이 없음

        // 이 큐의 랭크 게임을 시간 오름차순으로. 측정값도 오름차순이라 한 번의 선형 스캔으로 게임을 창에 배치한다.
        List<RankedGameView> games = participantRepository
                .findRankedGamesByPuuidAndQueueId(puuid, queueType.queueId());
        if (games.isEmpty()) return;

        int gameIndex = 0;
        for (int i = 1; i < queueReadings.size(); i++) {
            long windowStart = queueReadings.get(i - 1).getReadAtEpochMs(); // 배타
            long windowEnd = queueReadings.get(i).getReadAtEpochMs();       // 포함

            // 창 시작 이전(또는 같은 시각)의 게임은 이전 창들에서 이미 다뤘으니 건너뛴다.
            while (gameIndex < games.size()
                    && games.get(gameIndex).getGameCreation() <= windowStart) {
                gameIndex++;
            }

            // (windowStart, windowEnd] 에 들어오는 게임을 모은다.
            RankedGameView onlyGame = null;
            int countInWindow = 0;
            int scan = gameIndex;
            while (scan < games.size() && games.get(scan).getGameCreation() <= windowEnd) {
                onlyGame = games.get(scan);
                countInWindow++;
                scan++;
            }
            if (countInWindow != 1) continue; // 0판이면 귀속 대상 없음, 2판 이상이면 어느 판 몫인지 쪼갤 수 없음

            int lpChange = queueReadings.get(i).getLadderScore() - queueReadings.get(i - 1).getLadderScore();

            // 부호 정합성: 승리면 +, 패배면 - 여야 한다. 어긋나면(패배인데 +LP 등) 데이터가 꼬인 오귀속이라 버린다.
            Boolean win = onlyGame.getWin();
            if (win == null) continue;
            if (win && lpChange <= 0) continue;
            if (!win && lpChange >= 0) continue;

            lpChangeByMatchId.put(onlyGame.getMatchId(), lpChange);
        }
    }
}
