package com.recordsite.backend.dto;


import com.recordsite.backend.entity.Participant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantSummaryDto {

    private String puuid;
    private String gameName;
    private String tagLine;
    private int teamId;
    private boolean win;
    private int championId;
    private String championName;
    private int kills;
    private int deaths;
    private int assists;
    private String teamPosition;

    public static ParticipantSummaryDto from(Participant p) {
        return ParticipantSummaryDto.builder()
                .puuid(p.getPuuid())
                .gameName(p.getGameName())
                .tagLine(p.getTagLine())
                .teamId(p.getTeamId())
                .win(p.isWin())
                .championId(p.getChampionId())
                .championName(p.getChampionName())
                .kills(p.getKills())
                .deaths(p.getDeaths())
                .assists(p.getAssists())
                .teamPosition(p.getTeamPosition())
                .build();
    }
}
