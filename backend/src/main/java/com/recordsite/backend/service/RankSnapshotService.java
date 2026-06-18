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

    // 직전 스냅샷이 없는 매치(각 큐의 첫 스냅샷)는 맵에 담기지 않는다 = 프론트에서 LP 미표시
    @Transactional(readOnly = true)
    public Map<String, Integer> getLpChangesByPuuid(String puuid) {
        List<RankSnapshot> snapshots = rankSnapshotRepository.findByPuuidOrderByCreatedAtAsc(puuid);

        Map<QueueType, RankSnapshot> previousByQueue = new EnumMap<>(QueueType.class);
        Map<String, Integer> lpChangeByMatchId = new HashMap<>();

        for (RankSnapshot snapshot : snapshots) {
            RankSnapshot previous = previousByQueue.get(snapshot.getQueueType());
            if (previous != null) {
                int lpChange = snapshot.getLadderScore() - previous.getLadderScore();
                lpChangeByMatchId.put(snapshot.getAnchorMatchId(), lpChange);
            }
            previousByQueue.put(snapshot.getQueueType(), snapshot);
        }
        return lpChangeByMatchId;
    }
}
