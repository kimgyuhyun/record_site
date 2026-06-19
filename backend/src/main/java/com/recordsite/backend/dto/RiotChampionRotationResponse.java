package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Riot Champion-Rotation-V3 응답 (무료 로테이션 챔피언 키 목록)
@Getter
@Setter
@NoArgsConstructor
public class RiotChampionRotationResponse {
    private List<Integer> freeChampionIds;              // 전체 유저 무료 로테이션 챔피언 key 목록
    private List<Integer> freeChampionIdsForNewPlayers; // 신규 유저 전용 무료 로테이션
    private int maxNewPlayerLevel;                       // 신규 유저 로테이션 적용 최대 레벨
}
