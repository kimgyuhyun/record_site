package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Match;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchDto {

    private String matchId;
    private Long gameCreation;
    private Long gameDuration;
    private int queueId;
    private int mapId;
    private String gameMode;
    private String gameType;
    private String platformId;


    public static MatchDto from(Match match) {
        MatchDto dto = new MatchDto();
        dto.setMatchId(match.getMatchId());
        dto.setGameCreation(match.getGameCreation());
        dto.setGameDuration(match.getGameDuration());
        dto.setQueueId(match.getQueueId());
        dto.setMapId(match.getMapId());
        dto.setGameMode(match.getGameMode());
        dto.setGameType(match.getGameType());
        dto.setPlatformId(match.getPlatformId());

        return dto;
    }
}
