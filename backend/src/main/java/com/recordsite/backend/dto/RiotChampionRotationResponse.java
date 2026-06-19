package com.recordsite.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Riot Champion-Rotation-V3 응답 (무료 로테이션 챔피언 키 목록)
//  - Riot이 응답 필드를 sr/newplayer 로 바꿔서(구: freeChampionIds/freeChampionIdsForNewPlayers),
//    @JsonAlias 로 신·구 키를 모두 받아 어느 쪽이 와도 매핑되게 한다. maxNewPlayerLevel 은 더 이상 안 옴(0).
@Getter
@Setter
@NoArgsConstructor
public class RiotChampionRotationResponse {

    @JsonAlias("sr")
    private List<Integer> freeChampionIds;              // 전체 유저 무료 로테이션 챔피언 key 목록

    @JsonAlias("newplayer")
    private List<Integer> freeChampionIdsForNewPlayers; // 신규 유저 전용 무료 로테이션

    private int maxNewPlayerLevel;                       // 신규 유저 로테이션 적용 최대 레벨 (현재 응답엔 없음)
}
