package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Rune;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RuneDto {

    private Integer runeKey; // 룬 식별키

    private Integer pathKey; // 룬 경로 식별키

    private String runeNameEn;

    private String runeNameKor;

    private String image;

    private String longDesc;

    public static RuneDto from(Rune rune) {
        RuneDto dto = new RuneDto();
        dto.setRuneKey(rune.getRuneKey());
        dto.setPathKey(rune.getPath().getPathKey());
        dto.setRuneNameEn(rune.getRuneNameEn());
        dto.setRuneNameKor(rune.getRuneNameKor());
        dto.setImage(rune.getImage());
        dto.setLongDesc(rune.getLongDesc());
        return dto;
    }
}
