package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotLeagueEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeagueService {

    private final RiotLeagueClient riotLeagueClient;

    public List<RiotLeagueEntryResponse> getLeagueEntriesByPuuid(String puuid) {
        return riotLeagueClient.getLeagueEntriesByPuuid(puuid);
    }
}
