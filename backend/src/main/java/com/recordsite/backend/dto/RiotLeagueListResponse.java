package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

// Riot League-V4 by-queue 응답(챌린저/그랜드마스터/마스터 리그 1개의 전체 엔트리 목록).
@Getter
@Setter
@NoArgsConstructor
public class RiotLeagueListResponse {

    private String tier;              // CHALLENGER / GRANDMASTER / MASTER
    private List<Entry> entries;

    public List<Entry> safeEntries() {
        return entries == null ? Collections.emptyList() : entries;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Entry {
        private String summonerId;    // 암호화된 소환사 ID
        private String puuid;         // 최신 API 는 엔트리에 puuid 포함(이름 해소에 사용)
        private int leaguePoints;
        private String rank;          // 마스터+는 보통 "I"
        private int wins;
        private int losses;
    }
}
