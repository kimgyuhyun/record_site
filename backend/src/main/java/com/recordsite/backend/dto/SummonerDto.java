package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Summoner;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SummonerDto {


    private String puuid;
    private String name;
    private int profileIconId;
    private int level;
    private String tagLine;

    private String soloTier;
    private String soloRank;
    private Integer soloLp;
    private Integer soloWins;
    private Integer soloLosses;

    private String flexTier;
    private String flexRank;
    private Integer flexLp;
    private Integer flexWins;
    private Integer flexLosses;

    private LocalDateTime rankUpdatedAt;


    public static SummonerDto from(Summoner summoner) {
        SummonerDto dto = new SummonerDto();
        dto.setPuuid(summoner.getPuuid());
        dto.setName(summoner.getName());
        dto.setProfileIconId(summoner.getProfileIconId());
        dto.setLevel(summoner.getLevel());
        dto.setTagLine(summoner.getTagLine());
        dto.setSoloTier(summoner.getSoloTier());
        dto.setSoloRank(summoner.getSoloRank());
        dto.setSoloLp(summoner.getSoloLp());
        dto.setSoloWins(summoner.getSoloWins());
        dto.setSoloLosses(summoner.getSoloLosses());
        dto.setFlexTier(summoner.getFlexTier());
        dto.setFlexRank(summoner.getFlexRank());
        dto.setFlexLp(summoner.getFlexLp());
        dto.setFlexWins(summoner.getFlexWins());
        dto.setFlexLosses(summoner.getFlexLosses());
        dto.setRankUpdatedAt(summoner.getRankUpdatedAt());

        return dto;
    }
}
