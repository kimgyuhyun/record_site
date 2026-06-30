package com.recordsite.backend.service;

import com.recordsite.backend.dto.*;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.entity.Summoner;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final SummonerRepository summonerRepository;
    private final LpTimelineService lpTimelineService;

    @Transactional
    public void linkSummonerToParticipant(Participant participant) {
        if (participant.getSummoner() != null) return; // 이미 연결된 경우 방어합니다.

        Summoner s = summonerRepository.findBypuuid(participant.getPuuid());
        if (s == null) return; // db에 없으면 검색되지 않았으니 return 해서 null 유지합니다

        participant.setSummoner(s);
        participantRepository.save(participant);
    }


    private static final int QUEUE_SOLO = 420;
    private static final int QUEUE_FLEX = 440;

    @Transactional(readOnly = true)
    public Page<MatchRecordDto> findMatchRecordByPuuid(String puuid, Pageable pageable) {
        Page<MatchRecordDto> page = participantRepository.findMatchRecordByPuuid(puuid, pageable);

        // 이번 페이지에 포함된 matchId 목록 추출
        List<String> matchIds = page.getContent().stream()
                .map(MatchRecordDto::getMatchId)
                .toList();

        // matchId 목록으로 참가자 10명 한 번에 조회
        List<Participant> allParticipants = participantRepository.findByMatch_MatchIdIn(matchIds);

        // matchId → ParticipantSummaryDto 리스트 로 그룹핑
        Map<String, List<ParticipantSummaryDto>> participantMap = allParticipants.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getMatch().getMatchId(),
                        Collectors.mapping(ParticipantSummaryDto::from, Collectors.toList())
                ));

        // 소환사 랭크는 페이지 전체가 같은 puuid라 한 번만 조회 (없으면 null)
        Summoner summoner = summonerRepository.findBypuuid(puuid);

        // matchId -> 판당 LP 증감 (측정값으로 귀속 못 한 매치는 맵에 없음 = 미표시)
        Map<String, Integer> lpChangeByMatchId = lpTimelineService.getLpChangesByPuuid(puuid);

        // 각 MatchRecordDto에 participantSummaryDtos, killParticipation, tier, lpChange 주입
        page.getContent().forEach(dto -> {
            List<ParticipantSummaryDto> summaries = participantMap.getOrDefault(dto.getMatchId(), Collections.emptyList());
            dto.setParticipantSummaryDtos(summaries);

            int teamKills = dto.getTeamKills();
            double kp = teamKills == 0 ? 0.0
                    : (dto.getMyKills() + dto.getMyAssists()) * 100.0 / teamKills;
            dto.setMyKillParticipation(Math.round(kp * 10.0) / 10.0);

            injectTier(dto, summoner);
            dto.setMyLpChange(lpChangeByMatchId.get(dto.getMatchId()));
        });

        return page;
    }

    // 매치의 큐 종류(솔로/자유)에 맞는 소환사 티어/랭크를 주입한다.
    private void injectTier(MatchRecordDto dto, Summoner summoner) {
        RankedTier tier = resolveRankedTier(dto.getQueueId(), summoner);
        dto.setMyTier(tier.tier());
        dto.setMyRank(tier.rank());
    }

    // 큐 종류에 맞는 소환사 티어/랭크를 결정한다 (순수 함수 - 테스트 용이).
    // 랭크 큐가 아니거나 소환사 정보가 없으면 빈 티어를 반환한다.
    static RankedTier resolveRankedTier(int queueId, Summoner summoner) {
        if (summoner == null) return RankedTier.NONE;

        if (queueId == QUEUE_SOLO) {
            return new RankedTier(summoner.getSoloTier(), summoner.getSoloRank());
        }
        if (queueId == QUEUE_FLEX) {
            return new RankedTier(summoner.getFlexTier(), summoner.getFlexRank());
        }
        return RankedTier.NONE;
    }

    // 해당 큐 기준 소환사 랭크 (티어 + 단계)
    record RankedTier(String tier, String rank) {
        static final RankedTier NONE = new RankedTier(null, null);
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryDto> findParticipantSummaryListByMatchId(String matchId) {
        return participantRepository.findByMatchIdForParticipantList(matchId)
                .stream()
                .map(p -> MatchSummaryDto.from(p.getMatch(), p))
                .toList();
    }


}
