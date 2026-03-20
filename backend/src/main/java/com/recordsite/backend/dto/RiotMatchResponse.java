package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RiotMatchResponse {

    private Metadata metadata; // 최상위 메타데이터 (matchId, 참가자 puuid 목록)

    private Info info; // 실제 게임 정보 (시간/큐/맵/참가자 상세)


    @Getter
    @Setter
    @NoArgsConstructor
    public static class Metadata {
        private String matchId; // 매치 고유 id
        private List<String> participants = new ArrayList<>(); // 참가자 puuid 목록
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class Info {

        private long gameCreation;
        private long gameDuration;
        private int queueId;
        private int mapId;
        private String gameMode;
        private String gameType;

        private List<RiotParticipantResponse> participants = new ArrayList<>();

    }
}
