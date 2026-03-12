package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Champion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChampionSummaryDto {

    private String championId; // 영문 이름

    private Integer championKey; // API 식별키

    private String nameKor;
    private String nameEn;

    private String imageUrl;

    public static ChampionSummaryDto from(Champion champion) {
        ChampionSummaryDto dto = new ChampionSummaryDto();
        dto.setChampionId(champion.getChampionId());
        dto.setChampionKey(champion.getChampionKey());
        dto.setNameEn(champion.getNameEn());
        dto.setNameKor(champion.getNameKor());
        dto.setImageUrl(champion.getImageUrl());
        return dto;
    }
}
