package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Participant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RiotMatchResponse {
    private String matchId;
    private long gameCreation;
    private long gameDuration;
    private int queueId;
    private int mapId;
    private String gameMode;
    private String gameType;

    private List<RiotParticipantResponse> participantList = new ArrayList<>();

}
