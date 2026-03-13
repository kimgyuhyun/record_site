package com.recordsite.backend.dto;

import com.recordsite.backend.entity.RunePath;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RunePathDto {

    private Integer pathKey;

    private String runePathNameEn;

    private String runePathNameKor;

    private String image;

    public static RunePathDto from(RunePath runePath) {
        RunePathDto dto = new RunePathDto();
        dto.setPathKey(runePath.getPathKey());
        dto.setRunePathNameEn(runePath.getRunePathNameEn());
        dto.setRunePathNameKor(runePath.getRunePathNameKor());
        dto.setImage(runePath.getImage());

        return dto;
    }
}
