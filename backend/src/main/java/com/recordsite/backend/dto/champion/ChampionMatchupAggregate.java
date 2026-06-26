package com.recordsite.backend.dto.champion;

// 카운터(매치업) 집계용 projection. 같은 포지션의 상대 챔피언별로, 기준 챔피언이 몇 판 만나 몇 판 이겼는지.
public record ChampionMatchupAggregate(
        int championId,      // 상대 챔피언 id
        String championName, // 상대 챔피언 영문명
        long games,          // 기준 챔피언이 이 상대를 만난 판 수
        long wins            // 그중 기준 챔피언이 이긴 판 수
) {
}
