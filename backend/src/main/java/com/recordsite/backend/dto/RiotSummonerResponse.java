package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiotSummonerResponse {
    private String id; // summonerId
    private String puuid;
    private String name;
    private int profileIconId;
    private int summonerLevel;
}
