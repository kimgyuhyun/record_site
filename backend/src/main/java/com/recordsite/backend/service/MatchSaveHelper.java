package com.recordsite.backend.service;

import com.recordsite.backend.domain.ParticipantBuildOrder;
import com.recordsite.backend.dto.RiotMatchResponse;
import com.recordsite.backend.dto.RiotMatchTimelineResponse;
import com.recordsite.backend.dto.RiotParticipantResponse;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSaveHelper {

    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantService participantService;
    private final RiotMatchClient riotMatchClient;

    // matchId 하나를 받아서 Match + Participant 저장
    // REQUIRES_NEW로 매치 단위 독립 트랜잭션 - 실패해도 다른 매치에 영향 없음
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMatchWithParticipants(String matchId, String puuid) {

        // 1. Riot API 호출
        RiotMatchResponse res = riotMatchClient.getMatchById(matchId);
        if (res == null || res.getInfo() == null) {
            throw new IllegalStateException("Riot API 응답이 null. matchId=" + matchId);
        }

        // 2. 참가자 목록 추출
        List<RiotParticipantResponse> participants = res.getInfo().getParticipants();

        // 3. 팀별 teamKills 계산 (100 = 블루, 200 = 레드)
        Map<Integer, Integer> teamKillsMap = participants.stream()
                .collect(Collectors.groupingBy(
                   RiotParticipantResponse::getTeamId,
                   Collectors.summingInt(RiotParticipantResponse::getKills)
                ));

        // 4. Match 저장
        Match match = matchRepository.save(Match.from(res));

        // 4-1. 타임라인에서 참가자별 빌드 순서(아이템/스킬) 추출.
        //      타임라인은 부가 정보라 실패해도 매치 저장은 계속되어야 한다 → 절대 예외를 전파하지 않는다.
        Map<Integer, ParticipantBuildOrder> buildByParticipantId = fetchBuildOrders(matchId);

        // 5. Participant 생성 시 teamKills + 빌드 순서 주입 후 saveAll
        List<Participant> participantEntities = participants.stream()
                .map(rp -> {
                    Participant p = Participant.from(rp, match);
                    p.setTeamKills(teamKillsMap.getOrDefault(rp.getTeamId(), 0));
                    ParticipantBuildOrder build = buildByParticipantId.get(rp.getParticipantId());
                    if (build != null) {
                        p.setSkillBuildOrder(build.skillBuildOrder());
                        p.setItemBuildOrder(build.itemBuildOrder());
                    }
                    return p;
                })
                .toList();
        participantRepository.saveAll(participantEntities);


        // 6. 내 Participant에 Summoner 연결
        Participant me = participantRepository.findByMatchIdAndPuuid(matchId, puuid);
        if (me != null) {
            participantService.linkSummonerToParticipant(me);
        }
    }

    // 타임라인 호출 + 파싱. 실패(404/429/타임아웃/파싱오류 등) 시 빈 맵을 돌려주어 매치 저장에 영향 주지 않는다.
    private Map<Integer, ParticipantBuildOrder> fetchBuildOrders(String matchId) {
        try {
            RiotMatchTimelineResponse timeline = riotMatchClient.getMatchTimelineById(matchId);
            return ParticipantBuildOrder.parseByParticipantId(timeline);
        } catch (Exception e) {
            log.warn("타임라인 조회/파싱 실패, 빌드순서 없이 저장: matchId={}, error={}", matchId, e.getMessage());
            return Map.of();
        }
    }
}
