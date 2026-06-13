package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiotLeagueEntryResponse {
    private String queueType; // "RANKED_SOLO_5x5" or "RANKED_FLEX_SR"
    private String tier;
    private String rank; // "I", "II"
    private int leaguePoints;
    private int wins;
    private int losses;
}
