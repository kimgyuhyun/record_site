package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionBanCount;
import com.recordsite.backend.dto.champion.ChampionDetailResponse;
import com.recordsite.backend.dto.champion.ChampionMatchupAggregate;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.MatchBanRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 챔피언 상세(초상화 클릭) 집계. 우리가 수집한 매치 DB 를 챔피언 단위로 모아 룬/스킬/아이템/스펠/카운터/장인을 만든다.
 *
 * 표본이 OP.GG 처럼 크지 않으므로 수치는 얇을 수 있다(특히 카운터). 정적 데이터(스킬 설명/스킨)는 프론트가 DDragon 에서 받는다.
 */
@Service
@RequiredArgsConstructor
public class ChampionDetailService {

    private static final int PICKS_PER_MATCH = 10;

    // 상위 노출 개수
    private static final int TOP_RUNES = 5;
    private static final int TOP_SKILL_ORDERS = 3;
    private static final int TOP_ITEM_BUILDS = 3;
    private static final int TOP_SPELLS = 3;
    private static final int TOP_EXPERTS = 10;
    private static final int MAX_COUNTERS = 60;
    private static final int MIN_COUNTER_GAMES = 2; // 표본 1판짜리 매치업은 노이즈라 제외

    // 코어 아이템 빌드에서 제외할 소비/장신구 아이템(메타데이터가 없어 알려진 id 만 거른다).
    private static final Set<Integer> NON_CORE_ITEMS = Set.of(
            2003, 2031, 2033, 2055, 2138, 2139, 2140, 2150, 2151, 2152,
            3340, 3363, 3364, 3330, 2052, 2004, 2010, 3400);
    private static final int CORE_ITEM_COUNT = 3;

    private final ParticipantRepository participantRepository;
    private final MatchBanRepository matchBanRepository;

    @Transactional(readOnly = true)
    public ChampionDetailResponse getChampionDetail(int championId, QueueType queueType) {
        Integer queueId = queueType == null ? null : queueType.queueId();
        List<Participant> rows = participantRepository.findByChampionForStats(championId, queueId);

        long games = rows.size();
        long wins = rows.stream().filter(Participant::isWin).count();

        double totalMatches = (double) participantRepository.countParticipantsByQueue(queueId) / PICKS_PER_MATCH;
        long banCount = banCountOf(championId, queueId);

        return new ChampionDetailResponse(
                championId,
                championNameOf(rows),
                primaryPosition(rows),
                games,
                wins,
                ratio(wins, games),
                totalMatches <= 0 ? 0 : games / totalMatches,
                totalMatches <= 0 ? 0 : banCount / totalMatches,
                topRunes(rows),
                topSkillOrders(rows),
                topItemBuilds(rows),
                topSpells(rows),
                counters(championId, queueId),
                experts(rows));
    }

    // ── 요약 ──────────────────────────────────────────

    private String championNameOf(List<Participant> rows) {
        return rows.isEmpty() ? null : rows.get(0).getChampionName();
    }

    private String primaryPosition(List<Participant> rows) {
        Map<String, Long> byPosition = new LinkedHashMap<>();
        for (Participant p : rows) {
            String pos = p.getTeamPosition();
            if (pos == null || pos.isBlank()) {
                continue;
            }
            byPosition.merge(pos, 1L, Long::sum);
        }
        return byPosition.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private long banCountOf(int championId, Integer queueId) {
        return matchBanRepository.aggregateBanCounts(queueId).stream()
                .filter(b -> b.championId() == championId)
                .mapToLong(ChampionBanCount::banCount)
                .findFirst()
                .orElse(0L);
    }

    // ── 룬 ────────────────────────────────────────────

    private List<ChampionDetailResponse.RuneBuild> topRunes(List<Participant> rows) {
        Map<RuneKey, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            if (p.getKeystoneId() == null || p.getKeystoneId() == 0) {
                continue; // 룬 없는 모드(봇전/특수)·0으로 적재된 구버전 행 제외
            }
            RuneKey key = new RuneKey(
                    p.getPrimaryStyleId(), p.getKeystoneId(),
                    p.getPrimaryRune1(), p.getPrimaryRune2(), p.getPrimaryRune3(),
                    p.getSubStyleId(), p.getSubRune1(), p.getSubRune2(),
                    p.getStatPerkOffense(), p.getStatPerkFlex(), p.getStatPerkDefense());
            accumulate(tally, key, p.isWin());
        }
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_RUNES)
                .map(e -> {
                    RuneKey k = e.getKey();
                    long g = e.getValue()[0], w = e.getValue()[1];
                    return new ChampionDetailResponse.RuneBuild(
                            k.primaryStyleId(), k.keystoneId(),
                            k.primaryRune1(), k.primaryRune2(), k.primaryRune3(),
                            k.subStyleId(), k.subRune1(), k.subRune2(),
                            k.statOffense(), k.statFlex(), k.statDefense(),
                            g, w, ratio(w, g));
                })
                .toList();
    }

    private record RuneKey(
            Integer primaryStyleId, Integer keystoneId,
            Integer primaryRune1, Integer primaryRune2, Integer primaryRune3,
            Integer subStyleId, Integer subRune1, Integer subRune2,
            Integer statOffense, Integer statFlex, Integer statDefense) {}

    // ── 스킬 선마 순서 ─────────────────────────────────

    private List<ChampionDetailResponse.SkillOrder> topSkillOrders(List<Participant> rows) {
        Map<String, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            String order = skillMaxOrder(p.getSkillBuildOrder());
            if (order == null) {
                continue;
            }
            accumulate(tally, order, p.isWin());
        }
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_SKILL_ORDERS)
                .map(e -> new ChampionDetailResponse.SkillOrder(
                        e.getKey(), e.getValue()[0], e.getValue()[1], ratio(e.getValue()[1], e.getValue()[0])))
                .toList();
    }

    // "QWEQQR..." → 선마 순서 "Q>E>W" (R 제외). 각 스킬이 5포인트(=마스터)에 먼저 도달한 순.
    // 5포인트 미달 스킬은 뒤로(포인트 많은 순 → 먼저 찍은 순) 보낸다.
    private String skillMaxOrder(String build) {
        if (build == null || build.isBlank()) {
            return null;
        }
        char[] skills = {'Q', 'W', 'E'};
        int[] fifthIndex = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] count = new int[3];
        int[] firstIndex = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};

        for (int i = 0; i < build.length(); i++) {
            int s = indexOfSkill(skills, build.charAt(i));
            if (s < 0) {
                continue; // R 또는 그 외 문자
            }
            count[s]++;
            if (firstIndex[s] == Integer.MAX_VALUE) {
                firstIndex[s] = i;
            }
            if (count[s] == 5) {
                fifthIndex[s] = i;
            }
        }

        List<Integer> order = new ArrayList<>(List.of(0, 1, 2));
        order.sort(Comparator
                .comparingInt((Integer s) -> fifthIndex[s])      // 먼저 마스터한 순
                .thenComparing(s -> -count[s])                   // 포인트 많은 순
                .thenComparingInt(s -> firstIndex[s]));          // 먼저 찍은 순
        return skills[order.get(0)] + ">" + skills[order.get(1)] + ">" + skills[order.get(2)];
    }

    private int indexOfSkill(char[] skills, char c) {
        for (int i = 0; i < skills.length; i++) {
            if (skills[i] == c) {
                return i;
            }
        }
        return -1;
    }

    // ── 아이템 코어 빌드 ───────────────────────────────

    private List<ChampionDetailResponse.ItemBuild> topItemBuilds(List<Participant> rows) {
        Map<List<Integer>, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            List<Integer> core = coreItems(p.getItemBuildOrder());
            if (core.isEmpty()) {
                continue;
            }
            accumulate(tally, core, p.isWin());
        }
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_ITEM_BUILDS)
                .map(e -> new ChampionDetailResponse.ItemBuild(
                        e.getKey(), e.getValue()[0], e.getValue()[1], ratio(e.getValue()[1], e.getValue()[0])))
                .toList();
    }

    // "1055:8,2003:8,3006:520,..." → 소비/장신구 제외, 구매 순서대로 중복 없이 앞쪽 3개(코어).
    private List<Integer> coreItems(String itemBuildOrder) {
        if (itemBuildOrder == null || itemBuildOrder.isBlank()) {
            return List.of();
        }
        List<Integer> core = new ArrayList<>(CORE_ITEM_COUNT);
        for (String token : itemBuildOrder.split(",")) {
            int colon = token.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            int itemId;
            try {
                itemId = Integer.parseInt(token.substring(0, colon).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (NON_CORE_ITEMS.contains(itemId) || core.contains(itemId)) {
                continue;
            }
            core.add(itemId);
            if (core.size() == CORE_ITEM_COUNT) {
                break;
            }
        }
        return core;
    }

    // ── 소환사 주문 ────────────────────────────────────

    private List<ChampionDetailResponse.SpellPair> topSpells(List<Participant> rows) {
        Map<List<Integer>, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            int a = Math.min(p.getSpell1(), p.getSpell2());
            int b = Math.max(p.getSpell1(), p.getSpell2());
            if (a == 0 && b == 0) {
                continue;
            }
            accumulate(tally, List.of(a, b), p.isWin());
        }
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_SPELLS)
                .map(e -> new ChampionDetailResponse.SpellPair(
                        e.getKey().get(0), e.getKey().get(1),
                        e.getValue()[0], e.getValue()[1], ratio(e.getValue()[1], e.getValue()[0])))
                .toList();
    }

    // ── 카운터 ────────────────────────────────────────

    private List<ChampionDetailResponse.Counter> counters(int championId, Integer queueId) {
        return participantRepository.aggregateMatchups(championId, queueId).stream()
                .filter(m -> m.games() >= MIN_COUNTER_GAMES)
                .sorted(Comparator.comparingDouble((ChampionMatchupAggregate m) -> ratio(m.wins(), m.games())))
                .limit(MAX_COUNTERS)
                .map(m -> new ChampionDetailResponse.Counter(
                        m.championId(), m.championName(), m.games(), m.wins(), ratio(m.wins(), m.games())))
                .toList();
    }

    // ── 장인 랭킹 ──────────────────────────────────────

    private List<ChampionDetailResponse.Expert> experts(List<Participant> rows) {
        Map<String, long[]> games = new LinkedHashMap<>();
        Map<String, String[]> names = new LinkedHashMap<>(); // puuid → [gameName, tagLine]
        for (Participant p : rows) {
            String puuid = p.getPuuid();
            if (puuid == null) {
                continue;
            }
            accumulate(games, puuid, p.isWin());
            names.putIfAbsent(puuid, new String[]{p.getGameName(), p.getTagLine()});
        }
        return games.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_EXPERTS)
                .map(e -> {
                    String[] name = names.get(e.getKey());
                    long g = e.getValue()[0], w = e.getValue()[1];
                    return new ChampionDetailResponse.Expert(
                            e.getKey(), name[0], name[1], g, w, ratio(w, g));
                })
                .toList();
    }

    // ── 공통 유틸 ──────────────────────────────────────

    private <K> void accumulate(Map<K, long[]> tally, K key, boolean win) {
        long[] gw = tally.computeIfAbsent(key, k -> new long[2]);
        gw[0]++;            // games
        gw[1] += win ? 1 : 0; // wins
    }

    private <K> Comparator<Map.Entry<K, long[]>> byGamesDesc() {
        return Comparator.comparingLong((Map.Entry<K, long[]> e) -> e.getValue()[0]).reversed();
    }

    private double ratio(long part, long total) {
        return total <= 0 ? 0.0 : (double) part / total;
    }
}
