package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Summoner;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SummonerDto {


    private String puuid;

    private String name;

    private int profileIconId;

    private int level;

    private String tagLine;

    public static SummonerDto from(Summoner summoner) {
        SummonerDto dto = new SummonerDto();
        dto.setPuuid(summoner.getPuuid());
        dto.setName(summoner.getName());
        dto.setProfileIconId(summoner.getProfileIconId());
        dto.setLevel(summoner.getLevel());
        dto.setTagLine(summoner.getTagLine());

        return dto;
    }
}
