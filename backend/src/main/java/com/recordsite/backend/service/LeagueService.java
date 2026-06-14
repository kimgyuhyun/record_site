package com.recordsite.backend.service;

import com.recordsite.backend.dto.RiotLeagueEntryResponse;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeagueService {

    private final RiotLeagueClient riotLeagueClient;
    private final SummonerRepository summonerRepository;

    public Summoner updateAndSaveLeague(String puuid) {
        List<RiotLeagueEntryResponse> leagueEntries =
                riotLeagueClient.getLeagueEntriesByPuuid(puuid);
        Summoner summoner = summonerRepository.findBypuuid(puuid);

        for (RiotLeagueEntryResponse entry : leagueEntries) {
            if (entry.getQueueType().equals("RANKED_SOLO_5x5")) {
                summoner.setSoloTier(entry.getTier());
                summoner.setSoloRank(entry.getRank());
                summoner.setSoloLp(entry.getLeaguePoints());
                summoner.setSoloWins(entry.getWins());
                summoner.setSoloLosses(entry.getLosses());
            }

            if (entry.getQueueType().equals("RANKED_FLEX_SR")) {
                summoner.setFlexTier(entry.getTier());
                summoner.setFlexRank(entry.getRank());
                summoner.setFlexLp(entry.getLeaguePoints());
                summoner.setFlexWins(entry.getWins());
                summoner.setFlexLosses(entry.getLosses());
            }
        }
        summoner.setRankUpdatedAt(LocalDateTime.now());
        return summonerRepository.save(summoner);
    }

}
