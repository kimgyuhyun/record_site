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
        private String platformId;

        private List<RiotParticipantResponse> participants = new ArrayList<>();

        private List<Team> teams = new ArrayList<>();

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Team {
            private int teamId;
            private boolean win;

            private Objectives objectives; // 팀 오브젝트 획득 정보

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Objectives {
                private Objective baron; // 바론 획득 정보
                private Objective champion; // 챔피언 킬 합계 ( 팀 총 킬수)
                private Objective dragon; // 드래곤 획득 정보
                private Objective inhibitor; // 억제기 파괴 정보
                private Objective riftHerald; // 전령 획득 정보
                private Objective tower; // 타워 파괴 정보

                @Getter
                @Setter
                @NoArgsConstructor
                public static class Objective {
                    private boolean first;
                    private int kills;
                }
            }
        }
    }

}
