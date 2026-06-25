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

    // 아레나(CHERRY) 전용 — 비-아레나 매치에서는 null
    private Integer subteamPlacement; // 듀오의 최종 등수 = 1~4위
    private Integer playerSubteamId;  // 듀오 식별자(같은 값이면 한 팀)

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
                .subteamPlacement(p.getSubteamPlacement())
                .playerSubteamId(p.getPlayerSubteamId())
                .build();
    }
}
