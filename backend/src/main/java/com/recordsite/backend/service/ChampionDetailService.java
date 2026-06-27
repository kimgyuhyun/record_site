package com.recordsite.backend.service;

import com.recordsite.backend.dto.ChampionBanCount;
import com.recordsite.backend.dto.champion.ChampionDetailResponse;
import com.recordsite.backend.entity.Item;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.entity.QueueType;
import com.recordsite.backend.repository.ItemRepository;
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

    private static final int CORE_ITEM_COUNT = 3;        // 1·2·3 코어
    private static final int CORE_MIN_GOLD = 1000;        // 도란/시작템 같은 저가 완성템을 코어에서 제외하는 하한
    private static final int BOOTS_MIN_GOLD = 600;        // 기본 신발(1001, 300골드) 제외 → 2티어 신발만 신발 선택으로
    private static final int STARTING_SECONDS = 90;       // 게임 시작 후 이 시간(초) 내 구매만 시작 아이템 후보
    private static final int STARTING_GOLD_BUDGET = 650;  // 시작 골드로 살 수 있는 범위(도란+물약 등). 신발은 제외.

    private final ParticipantRepository participantRepository;
    private final MatchBanRepository matchBanRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public ChampionDetailResponse getChampionDetail(int championId, QueueType queueType) {
        Integer queueId = queueType == null ? null : queueType.queueId();
        List<Participant> rows = participantRepository.findByChampionForStats(championId, queueId);

        long games = rows.size();
        long wins = rows.stream().filter(Participant::isWin).count();

        double totalMatches = (double) participantRepository.countParticipantsByQueue(queueId) / PICKS_PER_MATCH;
        long banCount = banCountOf(championId, queueId);

        Map<Integer, ItemMeta> itemMeta = loadItemMeta();

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
                topStartingItems(rows, itemMeta),
                topBoots(rows, itemMeta),
                topCoreItems(rows, itemMeta),
                topSpells(rows));
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
        Map<String, long[]> tally = new LinkedHashMap<>();              // 선마순서 → [games, wins]
        Map<String, Map<String, Long>> levelSeqCount = new LinkedHashMap<>(); // 선마순서 → (레벨업순서 → 빈도)
        for (Participant p : rows) {
            String levels = normalizeLevels(p.getSkillBuildOrder());
            if (levels == null) {
                continue;
            }
            String order = skillMaxOrder(levels);
            if (order == null) {
                continue;
            }
            accumulate(tally, order, p.isWin());
            levelSeqCount.computeIfAbsent(order, k -> new LinkedHashMap<>()).merge(levels, 1L, Long::sum);
        }
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_SKILL_ORDERS)
                .map(e -> new ChampionDetailResponse.SkillOrder(
                        e.getKey(), mostCommonKey(levelSeqCount.get(e.getKey())),
                        e.getValue()[0], e.getValue()[1], ratio(e.getValue()[1], e.getValue()[0])))
                .toList();
    }

    // 레벨업 순서 문자열에서 Q/W/E/R 만 남기고 최대 18레벨까지 자른다.
    private String normalizeLevels(String build) {
        if (build == null || build.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < build.length() && sb.length() < 18; i++) {
            char c = build.charAt(i);
            if (c == 'Q' || c == 'W' || c == 'E' || c == 'R') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String mostCommonKey(Map<String, Long> counts) {
        return counts == null ? null : counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
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

    // ── 아이템: 시작 아이템 / 신발 / 핵심(1·2·3코어) ──────
    //  구매 순서 문자열(itemId:구매초)을 아이템 메타데이터(가격/태그/상위템 여부)로 분류해 집계한다.

    // 시작 아이템: 게임 시작 직후(STARTING_SECONDS 초 내), 시작 골드(STARTING_GOLD_BUDGET)로 살 수 있는 범위.
    //  - 신발·장신구는 제외(신발은 별도 섹션). 같은 구성끼리 묶어 상위 노출.
    private List<ChampionDetailResponse.ItemBuild> topStartingItems(
            List<Participant> rows, Map<Integer, ItemMeta> meta) {
        Map<List<Integer>, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            List<Integer> starting = new ArrayList<>();
            long spent = 0;
            for (Purchase buy : parsePurchases(p.getItemBuildOrder())) {
                if (buy.seconds() > STARTING_SECONDS) {
                    break; // 구매 순서대로라 시간 초과 시 이후는 볼 필요 없음
                }
                ItemMeta m = meta.get(buy.itemId());
                if (m == null || m.trinket() || m.boots() || starting.contains(buy.itemId())) {
                    continue;
                }
                if (spent + m.goldTotal() > STARTING_GOLD_BUDGET) {
                    break; // 시작 골드 예산 초과 → 이후 구매는 시작 아이템이 아님
                }
                starting.add(buy.itemId());
                spent += m.goldTotal();
            }
            if (!starting.isEmpty()) {
                accumulate(tally, starting, p.isWin());
            }
        }
        return topItemBuilds(tally);
    }

    // 신발: 구매한 첫 2티어 신발(기본 신발 1001 제외) 한 개를 집계.
    private List<ChampionDetailResponse.ItemBuild> topBoots(
            List<Participant> rows, Map<Integer, ItemMeta> meta) {
        Map<List<Integer>, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            for (Purchase buy : parsePurchases(p.getItemBuildOrder())) {
                ItemMeta m = meta.get(buy.itemId());
                if (m != null && m.boots() && m.goldTotal() >= BOOTS_MIN_GOLD) {
                    accumulate(tally, List.of(buy.itemId()), p.isWin());
                    break;
                }
            }
        }
        return topItemBuilds(tally);
    }

    // 핵심 빌드: 구매 순서대로 완성 아이템(상위템으로 안 올라가는 고가 아이템, 신발·소비·장신구 제외) 앞 3개.
    private List<ChampionDetailResponse.ItemBuild> topCoreItems(
            List<Participant> rows, Map<Integer, ItemMeta> meta) {
        Map<List<Integer>, long[]> tally = new LinkedHashMap<>();
        for (Participant p : rows) {
            List<Integer> core = new ArrayList<>(CORE_ITEM_COUNT);
            for (Purchase buy : parsePurchases(p.getItemBuildOrder())) {
                ItemMeta m = meta.get(buy.itemId());
                if (m == null || !m.completedCore() || core.contains(buy.itemId())) {
                    continue;
                }
                core.add(buy.itemId());
                if (core.size() == CORE_ITEM_COUNT) {
                    break;
                }
            }
            if (!core.isEmpty()) {
                accumulate(tally, core, p.isWin());
            }
        }
        return topItemBuilds(tally);
    }

    private List<ChampionDetailResponse.ItemBuild> topItemBuilds(Map<List<Integer>, long[]> tally) {
        return tally.entrySet().stream()
                .sorted(byGamesDesc())
                .limit(TOP_ITEM_BUILDS)
                .map(e -> new ChampionDetailResponse.ItemBuild(
                        e.getKey(), e.getValue()[0], e.getValue()[1], ratio(e.getValue()[1], e.getValue()[0])))
                .toList();
    }

    // "1055:8,2003:8,3006:520" → 구매(아이템id, 구매초) 목록(구매 순서 유지).
    private List<Purchase> parsePurchases(String itemBuildOrder) {
        if (itemBuildOrder == null || itemBuildOrder.isBlank()) {
            return List.of();
        }
        List<Purchase> purchases = new ArrayList<>();
        for (String token : itemBuildOrder.split(",")) {
            int colon = token.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            try {
                int itemId = Integer.parseInt(token.substring(0, colon).trim());
                long seconds = Long.parseLong(token.substring(colon + 1).trim());
                purchases.add(new Purchase(itemId, seconds));
            } catch (NumberFormatException ignored) {
                // 형식이 깨진 토큰은 건너뛴다
            }
        }
        return purchases;
    }

    private record Purchase(int itemId, long seconds) {}

    // 아이템 메타데이터(가격/신발·장신구 여부/완성 코어 여부)를 DB Item 에서 한 번에 적재한다.
    private Map<Integer, ItemMeta> loadItemMeta() {
        Map<Integer, ItemMeta> map = new LinkedHashMap<>();
        for (Item item : itemRepository.findAll()) {
            int id;
            try {
                id = Integer.parseInt(item.getItemKey());
            } catch (NumberFormatException e) {
                continue;
            }
            String tags = item.getTags() == null ? "" : item.getTags();
            boolean boots = tags.contains("Boots");
            boolean trinket = tags.contains("Trinket");
            boolean consumable = tags.contains("Consumable");
            boolean finished = item.getBuildsInto() == null || item.getBuildsInto().isBlank();
            boolean completedCore = item.isPurchasable() && finished && !boots && !trinket && !consumable
                    && item.getGoldTotal() >= CORE_MIN_GOLD;
            map.put(id, new ItemMeta(item.getGoldTotal(), boots, trinket, completedCore));
        }
        return map;
    }

    private record ItemMeta(int goldTotal, boolean boots, boolean trinket, boolean completedCore) {}

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
