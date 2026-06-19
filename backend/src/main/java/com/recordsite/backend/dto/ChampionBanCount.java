package com.recordsite.backend.dto;

// 챔피언별 밴된 매치 수(밴율 분자). 한 매치당 챔피언은 최대 1번 밴되므로 행 수 = 밴된 매치 수.
public record ChampionBanCount(
        Integer championId,
        Long banCount
) {
}
