package com.recordsite.backend.service;

import com.recordsite.backend.entity.*;
import com.recordsite.backend.repository.ChampionRepository;
import com.recordsite.backend.repository.ItemRepository;
import com.recordsite.backend.repository.RunePathRepository;
import com.recordsite.backend.repository.RuneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class DataDragonService {

    private final ChampionRepository championRepository;
    private final ItemRepository itemRepository;
    private final RestTemplate restTemplate;
    private final RunePathRepository runePathRepository;
    private final RuneRepository runeRepository;

    private static final String VERSION = "16.5.1";
    private static final String BASE_URL = "https://ddragon.leagueoflegends.com/cdn/" + VERSION;

    // 챔피언 목록 가져와서 하나씩 상세 JSON 호출 + 저장
    public void fetchAndSaveAllChampions() {
        String listUrl = BASE_URL + "/data/ko_KR/champion.json";
        Map<String, Object> response = restTemplate.getForObject(listUrl, Map.class);
        // restTemplate로 HTTP 요청 -> Map.Class로 JSON 전체를 Map으로 변환해서 받음
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");

        for (String championId : dataMap.keySet()) {
            if (championRepository.existsByChampionId(championId)) {
                log.info("이미 존재하는 챔피언: {}", championId);
                continue;
            }

            String detailUrl = BASE_URL + "/data/ko_KR/champion/" + championId + ".json";
            Map<String, Object> detailResponse = restTemplate.getForObject(detailUrl, Map.class);
            Map<String, Object> detailData = (Map<String, Object>) detailResponse.get("data");
            Map<String, Object> champData = (Map<String, Object>) detailData.get(championId);

            Champion champion = buildChampion(champData, championId);
            championRepository.save(champion);
            log.info("챔피언 저장 완료: {}", championId);
        }
    }


    // JSON → Champion 엔티티 변환
    private Champion buildChampion(Map<String, Object> data, String championId) {
        Champion champion = new Champion();
        champion.setChampionId(championId);
        champion.setChampionKey(Integer.parseInt((String) data.get("key")));
        champion.setNameEn(championId);
        champion.setNameKor((String) data.get("name"));
        champion.setTitle((String) data.get("title"));

        Map<String, Object> image = (Map<String, Object>) data.get("image");
        champion.setImageUrl((String) image.get("full"));

        List<String> tags = (List<String>) data.get("tags");
        champion.setTags(String.join(",", tags));

        champion.setPartype((String) data.get("partype"));
        champion.setBlurb((String) data.get("blurb"));
        champion.setLore((String) data.get("lore"));

        Map<String, Object> info = (Map<String, Object>) data.get("info");
        champion.setInfoAttack((int) info.get("attack"));
        champion.setInfoDefense((int) info.get("defense"));
        champion.setInfoMagic((int) info.get("magic"));
        champion.setInfoDifficulty((int) info.get("difficulty"));

        champion.setStats(buildStats(data, champion));
        champion.setSpells(buildSpells(data, champion));
        champion.setSkins(buildSkins(data, champion));

        return champion;
    }

    // JSON → ChampionStats 변환
    private ChampionStats buildStats(Map<String, Object> data, Champion champion) {
        Map<String, Object> s = (Map<String, Object>) data.get("stats");
        ChampionStats stats = new ChampionStats();
        stats.setChampion(champion);
        stats.setHp(toDouble(s.get("hp")));
        stats.setHpPerLevel(toDouble(s.get("hpperlevel")));
        stats.setMp(toDouble(s.get("mp")));
        stats.setMpPerLevel(toDouble(s.get("mpperlevel")));
        stats.setMoveSpeed(toDouble(s.get("movespeed")));
        stats.setArmor(toDouble(s.get("armor")));
        stats.setArmorPerLevel(toDouble(s.get("armorperlevel")));
        stats.setSpellBlock(toDouble(s.get("spellblock")));
        stats.setSpellBlockPerLevel(toDouble(s.get("spellblockperlevel")));
        stats.setAttackRange(toDouble(s.get("attackrange")));
        stats.setHpRegen(toDouble(s.get("hpregen")));
        stats.setHpRegenPerLevel(toDouble(s.get("hpregenperlevel")));
        stats.setMpRegen(toDouble(s.get("mpregen")));
        stats.setMpRegenPerLevel(toDouble(s.get("mpregenperlevel")));
        stats.setCrit(toDouble(s.get("crit")));
        stats.setCritPerLevel(toDouble(s.get("critperlevel")));
        stats.setAttackDamage(toDouble(s.get("attackdamage")));
        stats.setAttackDamagePerLevel(toDouble(s.get("attackdamageperlevel")));
        stats.setAttackSpeed(toDouble(s.get("attackspeed")));
        stats.setAttackSpeedPerLevel(toDouble(s.get("attackspeedperlevel")));
        return stats;
    }


    // JSON → ChampionSpell 리스트 변환 (패시브 + QWER)
    private List<ChampionSpell> buildSpells(Map<String, Object> data, Champion champion) {
        List<ChampionSpell> spellList = new ArrayList<>();

        // 패시브
        Map<String, Object> passive = (Map<String, Object>) data.get("passive");
        ChampionSpell passiveSpell = new ChampionSpell();
        passiveSpell.setChampion(champion);
        passiveSpell.setSlotIndex(0);
        passiveSpell.setName((String) passive.get("name"));
        passiveSpell.setDescription((String) passive.get("description"));
        Map<String, Object> passiveImage = (Map<String, Object>) passive.get("image");
        passiveSpell.setImageUrl((String) passiveImage.get("full"));
        spellList.add(passiveSpell);

        // QWER
        List<Map<String, Object>> spells = (List<Map<String, Object>>) data.get("spells");
        for (int i = 0; i < spells.size(); i++) {
            Map<String, Object> s = spells.get(i);
            ChampionSpell spell = new ChampionSpell();
            spell.setChampion(champion);
            spell.setSlotIndex(i + 1);
            spell.setSpellId((String) s.get("id"));
            spell.setName((String) s.get("name"));
            spell.setDescription((String) s.get("description"));
            spell.setMaxRank((int) s.get("maxrank"));
            spell.setCooldownBurn((String) s.get("cooldownBurn"));
            spell.setCostBurn((String) s.get("costBurn"));
            spell.setRangeBurn((String) s.get("rangeBurn"));
            Map<String, Object> spellImage = (Map<String, Object>) s.get("image");
            spell.setImageUrl((String) spellImage.get("full"));
            spellList.add(spell);
        }

        return spellList;
    }

    // JSON → ChampionSkin 리스트 변환
    private List<ChampionSkin> buildSkins(Map<String, Object> data, Champion champion) {
        List<ChampionSkin> skinList = new ArrayList<>();
        List<Map<String, Object>> skins = (List<Map<String, Object>>) data.get("skins");

        for (Map<String, Object> s : skins) {
            ChampionSkin skin = new ChampionSkin();
            skin.setChampion(champion);
            skin.setSkinId((String) s.get("id"));
            skin.setNum((int) s.get("num"));
            skin.setName((String) s.get("name"));
            skin.setChromas((boolean) s.get("chromas"));
            skinList.add(skin);
        }

        return skinList;
    }

    // JSON에서 int/double 섞여서 오는 거 처리
    private double toDouble(Object value) {
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Double) return (Double) value;
        return 0.0;
    }

    public void fetchAndSaveAllItems() {
        String url = BASE_URL + "/data/ko_KR/item.json";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");

        for (String itemKey : dataMap.keySet()) {
            if (itemRepository.existsByItemKey(itemKey)) {
                log.info("이미 존재하는 아이템: {}", itemKey);
                continue;
            }

            Map<String, Object> itemData = (Map<String, Object>) dataMap.get(itemKey);
            Item item = buildItem(itemData, itemKey);
            itemRepository.save(item);
            log.info("아이템 저장 완료: {} - {}", itemKey, item.getItemName());
        }
    }

    private Item buildItem(Map<String, Object> data, String itemKey) {
        Item item = new Item();
        item.setItemKey(itemKey);
        item.setItemName((String) data.get("name"));
        item.setDescription(data.get("description") != null ? (String) data.get("description") : "");
        item.setPlaintext((String) data.get("plaintext"));

        Map<String, Object> image = (Map<String, Object>) data.get("image");
        item.setImage((String) image.get("full"));

        List<String> intoList = (List<String>) data.get("into");
        if (intoList != null) {
            item.setBuildsInto(String.join(",", intoList));
        }

        Map<String, Object> gold = (Map<String, Object>) data.get("gold");
        item.setGoldBase((int) gold.get("base"));
        item.setGoldTotal((int) gold.get("total"));
        item.setGoldSell((int) gold.get("sell"));
        item.setPurchasable((boolean) gold.get("purchasable"));

        List<String> tags = (List<String>) data.get("tags");
        if (tags != null && !tags.isEmpty()) { // tags가 null이 아니고 비어있지 않으면
            item.setTags(String.join(",",tags));
        }
        return item;
    }

    public void fetchAndSaveAllRunes() {
        String url = BASE_URL + "/data/ko_KR/runesReforged.json";
        List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

        for (Map<String, Object> pathData : response) {
            Integer pathKey = (Integer) pathData.get("id");
            String runePathNameEn = (String) pathData.get("key");
            String runePathNameKor = (String) pathData.get("name");
            String icon = (String) pathData.get("icon");
            
            // 경로 저장 또는 조회
            RunePath runePath = runePathRepository.findByPathKey(pathKey)
                    .orElseGet(() -> { // orElseGet 메서드는 값이 있으면 그대로 리턴
                        // 값이 없으면 () -> { ... } 람다식을 실행해서 그 결과를 리턴함
                       RunePath rp = new RunePath();
                       rp.setPathKey(pathKey);
                       rp.setRunePathNameEn(runePathNameEn);
                       rp.setRunePathNameKor(runePathNameKor);
                       rp.setImage(icon);
                       return runePathRepository.save(rp);
                    });

            // slots -> runes 파싱
            List<Map<String, Object>> slots = (List<Map<String, Object>>) pathData.get("slots");
            for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                Map<String, Object> slot = slots.get(slotIndex);
                List<Map<String, Object>> runes = (List<Map<String, Object>>) slot.get("runes");

                for (Map<String, Object> runeData : runes) {
                    Integer runeKey = (Integer) runeData.get("id");
                    if (runeRepository.existsByRuneKey(runeKey)) {
                        continue;
                    }

                    Rune rune = new Rune();
                    rune.setRuneKey(runeKey);
                    rune.setRuneNameEn((String) runeData.get("key"));
                    rune.setRuneNameKor((String) runeData.get("name"));
                    rune.setImage((String) runeData.get("icon"));
                    rune.setShortDesc((String) runeData.get("shortDesc"));
                    rune.setLongDesc((String) runeData.get("longDesc"));
                    rune.setPath(runePath);

                    runeRepository.save(rune);
                }
            }
        }
    }
}
