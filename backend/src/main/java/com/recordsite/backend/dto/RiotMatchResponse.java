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

        private Long gameCreation;
        private Long gameDuration;
        private int queueId;
        private int mapId;
        private String gameMode;
        private String gameType;
        private String platformId;
        private String gameVersion; // 패치 버전(예: "14.12.123.4567") — 패치별 통계/필터용

        private List<RiotParticipantResponse> participants = new ArrayList<>();

        private List<Team> teams = new ArrayList<>();

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Team {
            private int teamId;
            private boolean win;

            private List<Ban> bans = new ArrayList<>(); // 이 팀이 밴한 챔피언 목록 (드래프트 없는 모드는 빈 목록)

            private Objectives objectives; // 팀 오브젝트 획득 정보

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Ban {
                private int championId; // 밴된 챔피언 id (밴 안 했으면 -1)
                private int pickTurn;   // 밴 순번
            }

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
