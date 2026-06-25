package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Match-V5 타임라인 응답. 분당 프레임 + 이벤트 중
// "아이템 구매 순서(ITEM_PURCHASED)" 와 "스킬 선마 순서(SKILL_LEVEL_UP)" 추출에 필요한 필드만 매핑한다.
// (나머지 100+ 필드는 Jackson 전역 설정상 무시 — RiotParticipantResponse 와 동일 방식)
@Getter
@Setter
@NoArgsConstructor
public class RiotMatchTimelineResponse {

    private Info info;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Info {
        private List<Frame> frames = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Frame {
        // 프레임은 시간순으로 내려오고, 프레임 안의 이벤트도 시간순이다 → 등장 순서가 곧 발생 순서.
        private List<Event> events = new ArrayList<>();
        // 분당 참가자별 스냅샷(골드/경험치 등). key = participantId("1".."10"). 골드 그래프용.
        private Map<String, ParticipantFrame> participantFrames;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ParticipantFrame {
        private Integer participantId;
        private Integer totalGold;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Position {
        private Integer x;
        private Integer y;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Event {
        private String type;            // "ITEM_PURCHASED", "SKILL_LEVEL_UP", "CHAMPION_KILL", "ELITE_MONSTER_KILL", "BUILDING_KILL" 등
        private Long timestamp;         // 게임 시작 후 경과 시간(ms)
        private Integer participantId;  // 이벤트 주체(1~10). 일부 글로벌 이벤트는 없을 수 있어 래퍼.
        private Integer itemId;         // ITEM_PURCHASED 시 구매한 아이템 id
        private Integer skillSlot;      // SKILL_LEVEL_UP 시 올린 스킬 슬롯(1=Q,2=W,3=E,4=R)
        private String levelUpType;     // "NORMAL" / "EVOLVE"

        // ── CHAMPION_KILL ──
        private Integer killerId;
        private Integer victimId;
        private List<Integer> assistingParticipantIds;
        private Position position;      // 발생 좌표(맵 표시용, 0~15000 범위)

        // ── ELITE_MONSTER_KILL ── (드래곤/전령/바론/공허유충)
        private Integer killerTeamId;
        private String monsterType;     // "DRAGON", "RIFTHERALD", "BARON_NASHOR", "HORDE"(공허 유충)
        private String monsterSubType;  // 드래곤 속성 등

        // ── BUILDING_KILL ── (포탑/억제기)
        private Integer teamId;         // 파괴된 건물 소속 팀
        private String buildingType;    // "TOWER_BUILDING", "INHIBITOR_BUILDING"
        private String laneType;        // "TOP_LANE" 등
        private String towerType;       // "OUTER_TURRET" 등
    }
}
