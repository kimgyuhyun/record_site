package com.recordsite.backend.service;

import com.recordsite.backend.dto.*;
import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    MatchRepository matchRepository;

    @Mock
    ParticipantRepository participantRepository;

    @Mock
    RiotMatchClient riotMatchClient;

    @Mock
    ParticipantService participantService;

    @InjectMocks
    MatchService matchService;


    private List<Participant> buildParticipantList(Match match, String myPuuid, int n) {
        List<Participant> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Participant p = new Participant();
            p.setMatch(match);
            p.setParticipantId(i + 1);
            p.setPuuid(i == 0 ? myPuuid : "other-" + i);
            p.setTeamId(100);
            p.setWin(true);
            p.setChampionId(1);
            p.setChampionName("Ahri");
            p.setTeamPosition("MID");
            p.setIndividualPosition("MID");
            p.setKills(1);
            p.setDeaths(0);
            p.setAssists(3);
            p.setGoldEarned(100);
            p.setTotalDamageDealt(1000L);
            p.setTotalDamageDealtToChampions(600L);
            p.setTotalDamageTaken(500L);
            p.setVisionScore(10);
            p.setChampionLevel(1);
            p.setStatPerkOffense(1);
            p.setStatPerkFlex(2);
            p.setStatPerkDefense(3);
            p.setItem0(101);
            p.setItem1(102);
            p.setItem2(103);
            p.setItem3(104);
            p.setItem4(105);
            p.setItem5(106);
            p.setItem6(107);
            p.setSpell1(11);
            p.setSpell2(22);
            list.add(p);
        }
        return list;
    }

    private RiotMatchResponse buildRiotMatchResponse(String matchId, String myPuuid) {
        RiotMatchResponse res = new RiotMatchResponse();
        RiotMatchResponse.Metadata metadata = new RiotMatchResponse.Metadata();
        metadata.setMatchId(matchId);
        RiotMatchResponse.Info info = new RiotMatchResponse.Info();
        info.setGameCreation(1L);
        info.setGameDuration(60L);
        info.setQueueId(420);
        info.setMapId(11);
        info.setGameMode("CLASSIC");
        info.setGameType("MATCHED_GAME");
        info.setPlatformId("KR1");
        List<RiotParticipantResponse> participantList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RiotParticipantResponse rp = new RiotParticipantResponse();
            rp.setParticipantId(i + 1);
            rp.setPuuid(i == 0 ? myPuuid : "other-" + i);
            rp.setRiotIdGameName("game-" + (i + 1));
            rp.setRiotIdTagline("KR1");
            rp.setTeamId(100);
            rp.setWin(i == 0);
            rp.setChampionId(1);
            rp.setChampionName("Ahri");
            rp.setTeamPosition("MID");
            rp.setIndividualPosition("MID");
            rp.setKills(1);
            rp.setDeaths(0);
            rp.setAssists(3);
            rp.setItem0(101);
            rp.setItem1(102);
            rp.setItem2(103);
            rp.setItem3(104);
            rp.setItem4(105);
            rp.setItem5(106);
            rp.setItem6(107);
            // Participant.from에서 사용하는 필드명(summoner1Id/2Id)로 세팅
            rp.setSummoner1Id(11);
            rp.setSummoner2Id(22);
            rp.setGoldEarned(100);
            rp.setTotalDamageDealt(1000L);
            rp.setTotalDamageDealtToChampions(600L);
            rp.setTotalDamageTaken(500L);
            rp.setVisionScore(10);
            rp.setChampLevel(1);
            participantList.add(rp);
        }
        info.setParticipants(participantList);
        res.setMetadata(metadata);
        res.setInfo(info);
        return res;
    }

    // DB에 매치에 대한 참가자가 10명 완전히 있을때 보강로직을 안타고 최종 DTO를 중복없이 리턴해줘야함
    @Test
    void getMatchListByPuuid_dbCOmplete_Test() {
        String puuid = "puuid1";

        Match m1 = new Match();
        Match m2 = new Match();
        Match m3 = new Match(); // 중복 체크용 match
        m1.setMatchId("1");
        m2.setMatchId("2");
        m3.setMatchId("2");

        Participant p1 = new Participant();
        Participant p2 = new Participant();
        Participant p3 = new Participant();
        p1.setPuuid(puuid);
        p2.setPuuid(puuid);
        p3.setPuuid(puuid);
        p1.setMatch(m1);
        p2.setMatch(m2);
        p3.setMatch(m3);


        when(participantRepository.findAllParticipantListByPuuid(puuid))
                .thenReturn(List.of(p1, p2, p3));

        when(riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20))
                .thenReturn(List.of("1", "2"));

        when(matchRepository.findByMatchId("1")).thenReturn(m1);
        when(matchRepository.findByMatchId("2")).thenReturn(m2);

        when(participantRepository.findByMatchIdForParticipantList("1"))
                .thenReturn(buildParticipantList(m1, puuid, 10));

        when(participantRepository.findByMatchIdForParticipantList("2"))
                .thenReturn(buildParticipantList(m2, puuid, 10));

        List<MatchListDto> matchListDtos = matchService.getMatchListByPuuid(puuid);

        List<String> matchIds = matchListDtos.stream()
                        .map(MatchListDto::getMatchId)
                        .toList();

        assertEquals(2, matchListDtos.size());
        assertEquals(matchListDtos.size(), matchIds.stream().distinct().count());
        assertEquals("1", matchListDtos.get(0).getMatchId());
        assertEquals("2", matchListDtos.get(1).getMatchId());
        assertEquals(puuid, matchListDtos.get(0).getMyPuuid());
        assertEquals(puuid, matchListDtos.get(1).getMyPuuid());

        // 참가자가 10명 다 저장되어잇으면 Riot 상세조회/저장 보강은 없어야 합니다
        // verify: 검증
        // never는 한번도 호출되면 안된다는 뜻
        verify(riotMatchClient).getMatchIdsByPuuid(puuid, 0, 20);
        verify(riotMatchClient, never()).getMatchById(anyString());
        verify(matchRepository, never()).save(any(Match.class));
        verify(participantRepository, never()).save(any(Participant.class));

        // Linke는 호출되어야 합니다. 매치 id 2개를 세팅해놔서 2번
        verify(participantService, times(2)).linkSummonerToParticipant(any(Participant.class));

    }


    // DB에 매치는 있는데 participant가 10명 미만이면 getmMachById로 보강하고, 최종 DTO는 중복없이 나와야함
    @Test
    @SuppressWarnings({"unchecked", "varargs"})
    void getMatchListByPuuid_dbIncomplete_test() {
        String puuid = "puuid1";

        Match m1 = new Match();
        Match m2 = new Match();
        m1.setMatchId("1");
        m2.setMatchId("1");

        Participant p1 = new Participant();
        Participant p2 = new Participant();
        p1.setPuuid(puuid);
        p2.setPuuid(puuid);
        p1.setMatch(m1);
        p2.setMatch(m2);

        when(participantRepository.findAllParticipantListByPuuid(puuid))
                .thenReturn(List.of(p1, p2));
        
        when(participantRepository.findByMatchIdForParticipantList("1"))
                .thenReturn(
                        buildParticipantList(m1, puuid, 5),
                        buildParticipantList(m1, puuid, 5),
                        buildParticipantList(m1, puuid, 10)
                );

        when(participantRepository.save(any(Participant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // any(Participant.class)는 save가 불릴 때 인자가 Participant타입이면 이 stub을 쓴다는 매칭 조건
        // thenRetunr(고정값) -> 호출될 때마다 항상 같은 객체를 돌려줌
        // thenAnswer(람다) -> 호출될 때마다 람다를 실행하고, 그 반환값을 save의 반환값처럼 사용
        // invocation.getArgument(0)) -> invocation으로부터 getArgument(0)으로 첫 번째 인자를 꺼낸다
        // invocation 자체는 이번 mock 호출에 대한 정보 묶음(어떤 mock, 어떤 메서드, 어떤 인자들로 불렸는지)
        // thenAnswer에 반환값 -> mcok이 찍어둔 save 메서드의 반환값이됨

        when(matchRepository.findByMatchId("1")).thenReturn(m1);

        when(riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20))
                .thenReturn(List.of("1"));

        when(riotMatchClient.getMatchById("1"))
                .thenReturn(buildRiotMatchResponse("1", puuid));

        when(participantRepository.existsByMatchAndParticipantId(eq(m1), anyInt()))
                .thenAnswer(invocation -> {
                    Integer participantId = invocation.getArgument(1);
                    return participantId <= 5;
                    // 1~5 true 중복, 이외 false 노중복
                });
        // eq(m1) -> 첫 번쨰 인자가 정확히 내가 만든 m1 인스턴스와 같으면 이 stub을 사용한다는 뜻
        // anyInt() -> 두 번째 인자는 어떤 int 값이든 가능하다는 뜻

        List<MatchListDto> matchListDtos = matchService.getMatchListByPuuid(puuid);

        List<String> matchIds = matchListDtos.stream()
                        .map(MatchListDto :: getMatchId)
                        .toList();

        assertEquals(1, matchListDtos.size());
        assertEquals(matchListDtos.size(), matchIds.stream().distinct().count());
        assertEquals("1", matchListDtos.get(0).getMatchId());
        assertEquals(puuid, matchListDtos.get(0).getMyPuuid());


        verify(riotMatchClient).getMatchIdsByPuuid(puuid, 0, 20);
        verify(riotMatchClient).getMatchById("1");
        verify(matchRepository, never()).save(any(Match.class));
        verify(participantRepository, times(5)).save(any(Participant.class));
        // save가 Participant 인자를 전달받으며 5번 호출됐는지 검증
        verify(participantService, times(1)).linkSummonerToParticipant(any(Participant.class));



    }

}