package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchTimelineDto;
import com.recordsite.backend.dto.RiotMatchTimelineResponse;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 타임라인 화면 데이터 조립자.
//  - 로스터(저장된 Participant)는 DB에서, 분당 골드/이벤트는 Riot 타임라인을 즉석(on-demand) 호출해 가공한다.
//  - 타임라인은 부가 화면이라 저장하지 않고 열 때마다 받아온다(저장 비용/용량 회피, dev key 레이트리밋은 공용 리미터가 처리).
@Service
@RequiredArgsConstructor
public class MatchTimelineService {

    private static final String CHAMPION_KILL = "CHAMPION_KILL";
    private static final String ELITE_MONSTER_KILL = "ELITE_MONSTER_KILL";
    private static final String BUILDING_KILL = "BUILDING_KILL";

    private final RiotMatchClient riotMatchClient;
    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public MatchTimelineDto getTimeline(String matchId) {
        List<Participant> participants = participantRepository.findByMatchIdForParticipantList(matchId);

        List<MatchTimelineDto.Roster> roster = participants.stream()
                .map(p -> new MatchTimelineDto.Roster(
                        p.getParticipantId(), p.getChampionId(), p.getChampionName(),
                        p.getTeamId(), p.getGameName(), p.getTagLine()))
                .sorted(Comparator.comparingInt(MatchTimelineDto.Roster::participantId))
                .toList();

        Map<Integer, Integer> teamByParticipantId = new HashMap<>();
        for (Participant p : participants) {
            teamByParticipantId.put(p.getParticipantId(), p.getTeamId());
        }

        RiotMatchTimelineResponse timeline = riotMatchClient.getMatchTimelineById(matchId);
        if (timeline == null || timeline.getInfo() == null) {
            return new MatchTimelineDto(roster, List.of(), List.of());
        }

        List<MatchTimelineDto.GoldFrame> goldFrames = buildGoldFrames(timeline, teamByParticipantId);
        List<MatchTimelineDto.Event> events = buildEvents(timeline);
        return new MatchTimelineDto(roster, goldFrames, events);
    }

    // 프레임마다 팀별 totalGold 합산 (프레임 index = 분).
    private List<MatchTimelineDto.GoldFrame> buildGoldFrames(
            RiotMatchTimelineResponse timeline, Map<Integer, Integer> teamByParticipantId) {

        List<MatchTimelineDto.GoldFrame> goldFrames = new ArrayList<>();
        int minute = 0;
        for (RiotMatchTimelineResponse.Frame frame : timeline.getInfo().getFrames()) {
            long blueGold = 0, redGold = 0;
            Map<String, RiotMatchTimelineResponse.ParticipantFrame> pf = frame.getParticipantFrames();
            if (pf != null) {
                for (Map.Entry<String, RiotMatchTimelineResponse.ParticipantFrame> entry : pf.entrySet()) {
                    Integer pid = parseIntOrNull(entry.getKey());
                    Integer gold = entry.getValue() == null ? null : entry.getValue().getTotalGold();
                    if (pid == null || gold == null) continue;
                    Integer team = teamByParticipantId.get(pid);
                    if (team == null) continue;
                    if (team == 100) blueGold += gold; else redGold += gold;
                }
            }
            goldFrames.add(new MatchTimelineDto.GoldFrame(minute++, blueGold, redGold));
        }
        return goldFrames;
    }

    // 킬/엘리트몬스터/건물파괴 이벤트만 추려 시간순 피드로.
    private List<MatchTimelineDto.Event> buildEvents(RiotMatchTimelineResponse timeline) {
        List<MatchTimelineDto.Event> events = new ArrayList<>();
        for (RiotMatchTimelineResponse.Frame frame : timeline.getInfo().getFrames()) {
            if (frame.getEvents() == null) continue;
            for (RiotMatchTimelineResponse.Event e : frame.getEvents()) {
                String type = e.getType();
                if (type == null) continue;
                long ts = e.getTimestamp() == null ? 0 : e.getTimestamp();
                Integer x = e.getPosition() == null ? null : e.getPosition().getX();
                Integer y = e.getPosition() == null ? null : e.getPosition().getY();

                if (CHAMPION_KILL.equals(type)) {
                    events.add(new MatchTimelineDto.Event(CHAMPION_KILL, ts,
                            e.getKillerId(), e.getVictimId(), e.getAssistingParticipantIds(),
                            null, null, null, null, null, null, x, y));
                } else if (ELITE_MONSTER_KILL.equals(type)) {
                    events.add(new MatchTimelineDto.Event(ELITE_MONSTER_KILL, ts,
                            e.getKillerId(), null, null,
                            e.getKillerTeamId(), e.getMonsterType(), e.getMonsterSubType(),
                            null, null, null, x, y));
                } else if (BUILDING_KILL.equals(type)) {
                    events.add(new MatchTimelineDto.Event(BUILDING_KILL, ts,
                            e.getKillerId(), null, e.getAssistingParticipantIds(),
                            e.getTeamId(), null, null, e.getBuildingType(), e.getLaneType(), e.getTowerType(), x, y));
                }
            }
        }
        return events;
    }

    private Integer parseIntOrNull(String s) {
        try { return Integer.valueOf(s); } catch (NumberFormatException ex) { return null; }
    }
}
