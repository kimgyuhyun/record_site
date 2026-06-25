package com.recordsite.backend.domain;

import com.recordsite.backend.dto.RiotMatchTimelineResponse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// 타임라인 이벤트에서 참가자별 "스킬 선마 순서" 와 "아이템 구매 순서" 를 뽑아낸 도메인 값 객체.
//  - skillBuildOrder: 레벨업 순서를 Q/W/E/R 문자로 이어 붙인 문자열 (예: "QWEQQRQ...") — 진화(EVOLVE)는 제외
//  - itemBuildOrder : 구매한 아이템 id 를 구매 순서대로 콤마로 이어 붙인 문자열 (소모품·장신구 포함, 가공은 프론트에 위임)
// 둘 다 추출된 이벤트가 없으면 null (= 데이터 없음, 빈 문자열과 구분).
public record ParticipantBuildOrder(String skillBuildOrder, String itemBuildOrder) {

    private static final String ITEM_PURCHASED = "ITEM_PURCHASED";
    private static final String SKILL_LEVEL_UP = "SKILL_LEVEL_UP";
    private static final String EVOLVE = "EVOLVE";

    // 슬롯(1~4) → 스킬 문자. 그 외 값은 매핑하지 않는다(방어).
    private static char skillLetter(int slot) {
        return switch (slot) {
            case 1 -> 'Q';
            case 2 -> 'W';
            case 3 -> 'E';
            case 4 -> 'R';
            default -> '?';
        };
    }

    // 타임라인 전체를 1회 순회하며 participantId 별로 스킬/아이템 순서를 누적해 Map 으로 반환.
    public static Map<Integer, ParticipantBuildOrder> parseByParticipantId(RiotMatchTimelineResponse timeline) {
        if (timeline == null || timeline.getInfo() == null) {
            return Map.of();
        }

        Map<Integer, StringBuilder> skillByPid = new LinkedHashMap<>();
        Map<Integer, StringBuilder> itemByPid = new LinkedHashMap<>();

        for (RiotMatchTimelineResponse.Frame frame : timeline.getInfo().getFrames()) {
            if (frame == null || frame.getEvents() == null) continue;

            for (RiotMatchTimelineResponse.Event event : frame.getEvents()) {
                if (event == null || event.getParticipantId() == null) continue;
                int pid = event.getParticipantId();
                String type = event.getType();
                if (type == null) continue;

                if (SKILL_LEVEL_UP.equals(type)) {
                    // 진화(카직스·빅토르 등)는 스킬 포인트 투자가 아니므로 선마 순서에서 제외
                    if (EVOLVE.equals(event.getLevelUpType())) continue;
                    Integer slot = event.getSkillSlot();
                    if (slot == null) continue;
                    char letter = skillLetter(slot);
                    if (letter == '?') continue; // 알 수 없는 슬롯은 버린다
                    skillByPid.computeIfAbsent(pid, k -> new StringBuilder()).append(letter);

                } else if (ITEM_PURCHASED.equals(type)) {
                    Integer itemId = event.getItemId();
                    if (itemId == null || itemId <= 0) continue;
                    StringBuilder sb = itemByPid.computeIfAbsent(pid, k -> new StringBuilder());
                    if (sb.length() > 0) sb.append(',');
                    sb.append(itemId);
                }
            }
        }

        Map<Integer, ParticipantBuildOrder> result = new HashMap<>();
        // 두 맵의 키 합집합으로 참가자별 값 객체 구성
        for (Integer pid : skillByPid.keySet()) {
            result.put(pid, build(skillByPid, itemByPid, pid));
        }
        for (Integer pid : itemByPid.keySet()) {
            result.computeIfAbsent(pid, p -> build(skillByPid, itemByPid, p));
        }
        return result;
    }

    private static ParticipantBuildOrder build(Map<Integer, StringBuilder> skillByPid,
                                               Map<Integer, StringBuilder> itemByPid,
                                               Integer pid) {
        StringBuilder skill = skillByPid.get(pid);
        StringBuilder item = itemByPid.get(pid);
        return new ParticipantBuildOrder(
                skill == null || skill.length() == 0 ? null : skill.toString(),
                item == null || item.length() == 0 ? null : item.toString());
    }
}
