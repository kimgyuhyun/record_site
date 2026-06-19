package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionPickCount;
import com.recordsite.backend.dto.RankingRowDto;
import com.recordsite.backend.dto.RiotAccountResponse;
import com.recordsite.backend.dto.RiotLeagueListResponse;
import com.recordsite.backend.entity.ApexTier;
import com.recordsite.backend.entity.LadderEntry;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.LadderEntryRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상위 티어 사다리 랭킹 — 주기적으로 League-V4 를 호출해 스냅샷을 갱신하고, 조회는 DB에서 한다.
 *
 * League-V4 는 이름(gameName#tagLine)을 주지 않으므로 account-v1 으로 해소해 비정규화 저장한다.
 * 매 갱신마다 기존 행의 이름을 캐시로 캐리오버해 신규 진입자만 재해소하므로, 호출량이 사다리 변동분으로
 * 한정된다. 모든 호출은 공유 레이트리밋을 통과해 dev-key 한도를 넘지 않는다(백그라운드로 천천히 채워짐).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";

    private final RiotLeagueClient riotLeagueClient;
    private final RiotSummonerClient riotSummonerClient;
    private final LadderEntryRepository ladderEntryRepository;
    private final ParticipantRepository participantRepository;

    @Value("${riot.ranking.enabled:true}")
    private boolean enabled;

    @Value("${riot.ranking.top-n:300}")
    private int topN;

    @Value("${riot.ranking.most-champion-count:7}")
    private int mostChampionCount;

    // ──────────────────────────────────────────
    // 주기 갱신 (실시간 아님 — 기본 1시간마다 스냅샷 교체)
    // ──────────────────────────────────────────
    @Scheduled(
            fixedDelayString = "${riot.ranking.refresh-interval-ms:3600000}",
            initialDelayString = "${riot.ranking.initial-delay-ms:60000}")
    public void refreshSoloLadder() {
        if (!enabled) {
            return;
        }
        try {
            refreshLadder(QueueType.SOLO, SOLO_QUEUE);
        } catch (Exception e) {
            log.warn("랭킹 갱신 실패: queue=SOLO, error={}", e.getMessage());
        }
    }

    private void refreshLadder(QueueType queueType, String riotQueue) {
        // 1. 챌린저/그랜드마스터/마스터 리그를 모아 평탄화
        List<RankedRow> rows = new ArrayList<>();
        for (ApexTier tier : ApexTier.values()) {
            RiotLeagueListResponse league = riotLeagueClient.getApexLeague(tier, riotQueue);
            if (league == null) {
                continue;
            }
            for (RiotLeagueListResponse.Entry e : league.safeEntries()) {
                rows.add(new RankedRow(tier, e.getPuuid(), e.getLeaguePoints(), e.getWins(), e.getLosses()));
            }
        }

        // 2. 사다리 정렬(티어 순서 → LP 내림차순) 후 상위 topN
        rows.sort(Comparator
                .comparingInt((RankedRow r) -> r.tier().order())
                .thenComparing(Comparator.comparingInt(RankedRow::leaguePoints).reversed()));
        List<RankedRow> top = rows.stream().limit(topN).toList();

        // 3. 기존 이름 캐시(puuid→[name,tag]) — 신규 진입자만 account-v1 재해소
        Map<String, String[]> nameCache = new HashMap<>();
        for (LadderEntry existing : ladderEntryRepository.findByQueueType(queueType)) {
            if (existing.getGameName() != null) {
                nameCache.put(existing.getPuuid(), new String[]{existing.getGameName(), existing.getTagLine()});
            }
        }

        // 4. 이름 해소하며 LadderEntry 생성
        List<LadderEntry> entries = new ArrayList<>(top.size());
        int rankPosition = 1;
        for (RankedRow r : top) {
            String[] name = resolveName(r.puuid(), nameCache);
            entries.add(LadderEntry.of(
                    queueType, r.tier(), rankPosition++, r.puuid(),
                    name == null ? null : name[0],
                    name == null ? null : name[1],
                    r.leaguePoints(), r.wins(), r.losses()));
        }

        // 5. 해당 큐 스냅샷 교체
        ladderEntryRepository.deleteByQueueType(queueType);
        ladderEntryRepository.saveAll(entries);
        log.info("랭킹 갱신 완료: queue={}, {}명", queueType, entries.size());
    }

    // 캐시에 있으면 재사용, 없으면 account-v1 으로 해소. 실패 시 null(이름 미표시).
    private String[] resolveName(String puuid, Map<String, String[]> nameCache) {
        if (puuid == null) {
            return null;
        }
        String[] cached = nameCache.get(puuid);
        if (cached != null) {
            return cached;
        }
        try {
            RiotAccountResponse account = riotSummonerClient.getAccountByPuuid(puuid);
            if (account == null || account.getGameName() == null) {
                return null;
            }
            String[] resolved = {account.getGameName(), account.getTagLine()};
            nameCache.put(puuid, resolved);
            return resolved;
        } catch (Exception e) {
            log.debug("랭킹 이름 해소 실패: puuid={}", puuid);
            return null;
        }
    }

    // ──────────────────────────────────────────
    // 조회 전용 - DB에서 페이지 단위 + 페이지 puuid의 모스트 챔피언만 계산
    // ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<RankingRowDto> getRanking(QueueType queueType, Pageable pageable) {
        Page<LadderEntry> page = ladderEntryRepository
                .findByQueueTypeOrderByRankPositionAsc(queueType, pageable);

        List<String> pagePuuids = page.getContent().stream()
                .map(LadderEntry::getPuuid)
                .toList();
        Map<String, List<Integer>> mostChampionsByPuuid = topChampionsByPuuid(pagePuuids);

        return page.map(entry ->
                RankingRowDto.of(entry, mostChampionsByPuuid.getOrDefault(entry.getPuuid(), List.of())));
    }

    // 페이지에 보이는 puuid들에 대해서만 우리 DB 기준 상위 픽 championId 목록을 만든다.
    private Map<String, List<Integer>> topChampionsByPuuid(List<String> puuids) {
        if (puuids.isEmpty()) {
            return Map.of();
        }
        Map<String, List<ChampionPickCount>> byPuuid = participantRepository.findChampionPickCounts(puuids).stream()
                .collect(Collectors.groupingBy(ChampionPickCount::puuid));

        Map<String, List<Integer>> result = new HashMap<>();
        byPuuid.forEach((puuid, picks) -> {
            List<Integer> championIds = picks.stream()
                    .sorted(Comparator.comparingLong(ChampionPickCount::games).reversed())
                    .limit(mostChampionCount)
                    .map(ChampionPickCount::championId)
                    .toList();
            result.put(puuid, championIds);
        });
        return result;
    }

    // 정렬 전용 평탄화 행(티어 + LP + 전적).
    private record RankedRow(ApexTier tier, String puuid, int leaguePoints, int wins, int losses) {
    }
}
