package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Event {
        private String type;            // "ITEM_PURCHASED", "SKILL_LEVEL_UP" 등
        private Integer participantId;  // 이벤트 주체(1~10). 일부 글로벌 이벤트는 없을 수 있어 래퍼.
        private Integer itemId;         // ITEM_PURCHASED 시 구매한 아이템 id
        private Integer skillSlot;      // SKILL_LEVEL_UP 시 올린 스킬 슬롯(1=Q,2=W,3=E,4=R)
        private String levelUpType;     // "NORMAL"(일반 투자) / "EVOLVE"(카직스·빅토르 진화 등)
    }
}
