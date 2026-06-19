package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionBanCount;
import com.recordsite.backend.dto.ChampionPositionAggregate;
import com.recordsite.backend.dto.ChampionTierRowDto;
import com.recordsite.backend.dto.PlayedChampionStatDto;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.MatchBanRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 챔피언 통계 조회 (읽기 전용). 소환사별 플레이 통계 + 전역 티어 리스트.
@Service
@RequiredArgsConstructor
public class ChampionStatService {

    // 매치당 픽 수(랭크 기준). 전체 픽 합÷10 ≈ 전체 매치 수 → 픽률 분모로 사용.
    private static final int PICKS_PER_MATCH = 10;

    private final ParticipantRepository participantRepository;
    private final MatchBanRepository matchBanRepository;

    @Transactional(readOnly = true)
    public List<PlayedChampionStatDto> getPlayedChampions(String puuid, QueueType queueType) {
        Integer queueId = queueType == null ? null : queueType.queueId();
        return participantRepository.aggregatePlayedChampions(puuid, queueId).stream()
                .map(PlayedChampionStatDto::from)
                .toList();
    }

    // 전역 챔피언 티어 리스트. 자체 수집한 매치 DB를 챔피언 단위로 집계해 승률/픽률/티어를 만든다.
    @Transactional(readOnly = true)
    public List<ChampionTierRowDto> getChampionTierList(QueueType queueType) {
        Integer queueId = queueType == null ? null : queueType.queueId();
        List<ChampionPositionAggregate> rows = participantRepository.aggregateChampionStatsByPosition(queueId);

        long totalPicks = rows.stream().mapToLong(ChampionPositionAggregate::games).sum();
        long totalMatches = totalPicks / PICKS_PER_MATCH;

        // 챔피언별 밴된 매치 수(밴율 분자). 같은 큐 필터로 집계한다.
        Map<Integer, Long> banCountByChampion = matchBanRepository.aggregateBanCounts(queueId).stream()
                .collect(Collectors.toMap(ChampionBanCount::championId, ChampionBanCount::banCount));

        // 챔피언 단위로 포지션을 합산하고, 가장 많이 플레이된 포지션을 주 포지션으로 잡는다.
        Map<Integer, List<ChampionPositionAggregate>> byChampion = rows.stream()
                .collect(Collectors.groupingBy(ChampionPositionAggregate::championId));

        List<ChampionTierRowDto> tierRows = new ArrayList<>(byChampion.size());
        for (List<ChampionPositionAggregate> positions : byChampion.values()) {
            long games = positions.stream().mapToLong(ChampionPositionAggregate::games).sum();
            long wins = positions.stream().mapToLong(ChampionPositionAggregate::wins).sum();
            ChampionPositionAggregate primary = positions.stream()
                    .max(Comparator.comparingLong(ChampionPositionAggregate::games))
                    .orElse(positions.get(0));
            long banCount = banCountByChampion.getOrDefault(primary.championId(), 0L);

            tierRows.add(ChampionTierRowDto.of(
                    primary.championId(), primary.championName(), primary.teamPosition(),
                    games, wins, banCount, totalMatches));
        }

        tierRows.sort(Comparator.comparingDouble(ChampionTierRowDto::score).reversed());
        return assignTiers(tierRows);
    }

    // score 내림차순으로 정렬된 리스트에 백분위 기반 티어(OP/1~4)를 부여한다.
    private List<ChampionTierRowDto> assignTiers(List<ChampionTierRowDto> sortedByScoreDesc) {
        int total = sortedByScoreDesc.size();
        List<ChampionTierRowDto> withTiers = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            double percentile = total <= 1 ? 0.0 : (double) i / total;
            withTiers.add(sortedByScoreDesc.get(i).withTier(tierLabel(percentile)));
        }
        return withTiers;
    }

    private String tierLabel(double percentile) {
        if (percentile < 0.10) return "OP";
        if (percentile < 0.25) return "1";
        if (percentile < 0.50) return "2";
        if (percentile < 0.80) return "3";
        return "4";
    }
}
