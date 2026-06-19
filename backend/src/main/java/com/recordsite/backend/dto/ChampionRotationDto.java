package com.recordsite.backend.dto;

import java.util.Collections;
import java.util.List;

// 무료 로테이션 챔피언 응답 DTO (프론트 홈 로테이션 섹션용)
//  - championId(int) 목록만 전달하고, 아이콘/이름 매핑은 프론트의 Data Dragon 메타로 처리한다.
public record ChampionRotationDto(
        List<Integer> freeChampionIds,
        List<Integer> freeChampionIdsForNewPlayers,
        int maxNewPlayerLevel
) {
    public static ChampionRotationDto from(RiotChampionRotationResponse res) {
        if (res == null) {
            return new ChampionRotationDto(Collections.emptyList(), Collections.emptyList(), 0);
        }
        return new ChampionRotationDto(
                res.getFreeChampionIds() == null ? Collections.emptyList() : res.getFreeChampionIds(),
                res.getFreeChampionIdsForNewPlayers() == null ? Collections.emptyList() : res.getFreeChampionIdsForNewPlayers(),
                res.getMaxNewPlayerLevel()
        );
    }
}
