package com.recordsite.backend.service;

import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.entity.RankSnapshot;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.RankSnapshotRepository;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 판당 LP 증감(스냅샷 비교 방식)의 쓰기/읽기 조율자.
// - 쓰기: 갱신 시점의 현재 LP를 각 큐의 최신 랭크 매치에 앵커로 박아 스냅샷 저장
// - 읽기: 인접 스냅샷의 ladderScore 차이로 matchId -> LP 증감 맵 생성
@Service
@RequiredArgsConstructor
public class RankSnapshotService {

    private final RankSnapshotRepository rankSnapshotRepository;
    private final ParticipantRepository participantRepository;
    private final SummonerRepository summonerRepository;

    @Transactional
    public void recordSnapshots(String puuid) {
        Summoner summoner = summonerRepository.findBypuuid(puuid);
        if (summoner == null) return;

        recordForQueue(puuid, QueueType.SOLO,
                summoner.getSoloTier(), summoner.getSoloRank(), summoner.getSoloLp());
        recordForQueue(puuid, QueueType.FLEX,
                summoner.getFlexTier(), summoner.getFlexRank(), summoner.getFlexLp());
    }

    private void recordForQueue(String puuid, QueueType queueType,
                                String tier, String division, Integer leaguePoints) {
        if (tier == null || leaguePoints == null) return; // 언랭 큐는 스냅샷 없음

        String anchorMatchId = findLatestRankedMatchId(puuid, queueType.queueId());
        if (anchorMatchId == null) return; // 해당 큐 랭크 매치가 아직 없음

        boolean alreadyAnchored = rankSnapshotRepository
                .existsByPuuidAndQueueTypeAndAnchorMatchId(puuid, queueType, anchorMatchId);
        if (alreadyAnchored) return; // 같은 매치에 이미 스냅샷이 있으면 중복 저장 안 함

        rankSnapshotRepository.save(
                RankSnapshot.of(puuid, queueType, tier, division, leaguePoints, anchorMatchId));
    }

    private String findLatestRankedMatchId(String puuid, int queueId) {
        List<String> matchIds = participantRepository
                .findMatchIdsByPuuidAndQueueId(puuid, queueId, PageRequest.of(0, 1));
        return matchIds.isEmpty() ? null : matchIds.get(0);
    }

    // 판당 LP 증감 맵(matchId -> ±LP). 다음 조건을 모두 만족하는 매치에만 값이 담긴다(나머지는 프론트에서 미표시):
    //  1) 같은 큐의 직전 스냅샷이 존재한다(각 큐 첫 스냅샷은 비교 대상이 없음)
    //  2) 직전 앵커와 현재 앵커 사이에 낀 랭크 게임이 정확히 한 판이다
    // (2)가 핵심: 두 갱신 사이에 여러 판을 했다면 LP 차이가 어느 판 몫인지 쪼갤 수 없으므로, 통째로 한 판에
    // 귀속시키면 패배인데 +LP가 찍히는 식의 오귀속이 생긴다. 단판으로 끊겼을 때만 그 차이를 그 판에 귀속한다.
    @Transactional(readOnly = true)
    public Map<String, Integer> getLpChangesByPuuid(String puuid) {
        List<RankSnapshot> snapshots = rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(puuid);

        Map<QueueType, RankSnapshot> previousByQueue = new EnumMap<>(QueueType.class);
        Map<String, Integer> lpChangeByMatchId = new HashMap<>();

        for (RankSnapshot snapshot : snapshots) {
            RankSnapshot previous = previousByQueue.get(snapshot.getQueueType());
            previousByQueue.put(snapshot.getQueueType(), snapshot);
            if (previous == null) continue;

            Long previousAnchorTime = participantRepository
                    .findGameCreationByPuuidAndMatchId(puuid, previous.getAnchorMatchId());
            Long currentAnchorTime = participantRepository
                    .findGameCreationByPuuidAndMatchId(puuid, snapshot.getAnchorMatchId());
            if (previousAnchorTime == null || currentAnchorTime == null) continue;

            long rankedGamesBetween = participantRepository.countRankedMatchesInWindow(
                    puuid, snapshot.getQueueType().queueId(), previousAnchorTime, currentAnchorTime);
            if (rankedGamesBetween != 1) continue; // 단판으로 끊긴 구간만 귀속

            int lpChange = snapshot.getLadderScore() - previous.getLadderScore();

            // 부호 정합성: 승리면 +, 패배면 - 여야 한다. 어긋나면(패배인데 +LP 등) 데이터가 꼬인 오귀속이므로 버린다.
            Boolean win = participantRepository
                    .findWinByPuuidAndMatchId(puuid, snapshot.getAnchorMatchId());
            if (win == null) continue;
            if (win && lpChange <= 0) continue;
            if (!win && lpChange >= 0) continue;

            lpChangeByMatchId.put(snapshot.getAnchorMatchId(), lpChange);
        }
        return lpChangeByMatchId;
    }
}
