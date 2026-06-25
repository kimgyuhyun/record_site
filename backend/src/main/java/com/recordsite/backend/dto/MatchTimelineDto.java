package com.recordsite.backend.dto;

import java.util.List;

// 타임라인 화면(맵/이벤트 피드/골드 그래프)용 가공 DTO.
//  - participants: 참가자 로스터(participantId → 챔피언/팀/소환사명)
//  - goldFrames  : 분당 팀 합산 골드(블루/레드) — 골드 차이 그래프용
//  - events      : 킬/오브젝트/건물 파괴 이벤트(시간순) — 이벤트 피드 + 맵 마커용
public record MatchTimelineDto(
        List<Roster> participants,
        List<GoldFrame> goldFrames,
        List<Event> events
) {
    public record Roster(
            int participantId, int championId, String championName,
            int teamId, String gameName, String tagLine
    ) {}

    public record GoldFrame(int minute, long blueGold, long redGold) {}

    // category: "CHAMPION_KILL" | "ELITE_MONSTER_KILL" | "BUILDING_KILL"
    public record Event(
            String category,
            long timestamp,            // ms
            Integer killerId,          // participantId (없을 수 있음 = 처형/미니언 등)
            Integer victimId,          // 챔피언 킬 피해자
            List<Integer> assistIds,
            Integer teamId,            // ELITE: 처치 팀 / BUILDING: 파괴된 건물 소속 팀
            String monsterType,        // ELITE_MONSTER_KILL
            String monsterSubType,
            String buildingType,       // BUILDING_KILL
            String laneType,
            String towerType,
            Integer x, Integer y       // 맵 좌표
    ) {}
}
