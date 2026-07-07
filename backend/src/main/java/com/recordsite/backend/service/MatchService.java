package com.recordsite.backend.service;

import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.MatchSummaryDto;
import com.recordsite.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final ParticipantService participantService;
    private final MatchSaveHelper matchSaveHelper;
    private final RiotMatchClient riotMatchClient;
    private final LeagueService leagueService;
    private final SummonerCrawlService summonerCrawlService;
    private final StalePuuidHealer stalePuuidHealer;
    private final Executor matchCollectExecutor;


    // ──────────────────────────────────────────
    // 조회 전용 - Riot API 호출 없음
    // ──────────────────────────────────────────

    // DB에서 페이지 단위로 전적 목록 반환
    @Transactional(readOnly = true)
    public Page<MatchRecordDto> getMatchRecordsByPuuid(String puuid, Pageable pageable) {
        return participantService.findMatchRecordByPuuid(puuid, pageable);
    }

    // 해당 판 전체 참가자 상세 정보 반환
    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getParticipantSummaryListByMatchId(String matchId) {
        return participantService.findParticipantSummaryListByMatchId(matchId);

    }

    // ──────────────────────────────────────────
    // 갱신 전용 - 전적갱신 작업 큐의 워커가 호출
    // ──────────────────────────────────────────

    // Riot API에서 최근 전적 20개 받아서 DB에 없는 것만 저장(증분 수집), 자유/솔로 랭크도 갱신.
    // 진행 상황은 progress 콜백으로 흘려보낸다(워커가 잡 상태에 반영). 새로 저장된 매치 수 반환.
    // 중복 갱신 차단·쿨다운은 호출 측의 Redis 락(RefreshJobStore)이 담당하므로 여기선 검사하지 않는다.
    public int refreshMatchesByPuuid(String puuid, RefreshProgress progress) {

        // 라이엇 API 에서 최근 매치 20개 조회.
        // 저장된 puuid 가 Riot 에서 복호화 불가(계정 puuid 변경)면 400 이 온다 →
        // 이름#태그로 현재 puuid 를 재해소·이관(자가치유)한 뒤 그 puuid 로 재시도한다.
        List<String> matchIdList;
        try {
            matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
        } catch (HttpClientErrorException.BadRequest e) {
            String healedPuuid = stalePuuidHealer.heal(puuid);
            if (healedPuuid == null) {
                throw e; // 치유할 수 없으면 원래 오류를 그대로 전파
            }
            puuid = healedPuuid;
            matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
        }
        if (matchIdList.isEmpty()) {
            progress.onTotal(0);
            return 0;
        }

        // DB에 이미 있는 matchId를 한 번의 쿼리로 조회 -> Set 으로 변환
        Set<String> existingIds = matchRepository.findExistingMatchIds(matchIdList);

        List<String> newMatchIds = matchIdList.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        progress.onTotal(newMatchIds.size());

        // db에 없는 매치를 동시에 저장(매치당 독립 트랜잭션 REQUIRES_NEW). 동시 개수는 matchCollectExecutor 풀 크기가 제한.
        int newCount = collectNewMatches(newMatchIds, puuid, progress);

        // 랭크(티어/LP) 갱신은 신규 매치 유무와 무관하게 매 갱신마다 수행한다(프로필의 현재 랭크 표시용).
        leagueService.updateAndSaveLeague(puuid);

        // 이번에 새로 만난 동료 puuid 들만 크롤러 큐에 적재 → 백그라운드로 매치망 점진 확장(챔피언 통계 표본 확보)
        if (!newMatchIds.isEmpty()) {
            summonerCrawlService.enqueueNeighborsFromMatches(newMatchIds, 1, puuid);
        }

        return newCount;
    }

    // 신규 매치들을 matchCollectExecutor 풀에서 동시에 수집한다. 매치당 호출은 REQUIRES_NEW 라 서로 독립이고,
    // 한 건 실패는 그 매치만 스킵한다. 모든 작업이 끝날 때까지 기다린 뒤 실제 저장된 수를 반환한다.
    private int collectNewMatches(List<String> newMatchIds, String puuid, RefreshProgress progress) {
        AtomicInteger savedCount = new AtomicInteger();

        CompletableFuture<?>[] tasks = newMatchIds.stream()
                .map(matchId -> CompletableFuture.runAsync(() -> {
                    try {
                        matchSaveHelper.saveMatchWithParticipants(matchId, puuid);
                        savedCount.incrementAndGet();
                    } catch (Exception e) {
                        log.warn("매치 저장 실패, 스킵: matchId={}, error={}", matchId, e.getMessage());
                    } finally {
                        progress.onMatchDone(); // 성공·스킵 무관하게 진행률 한 칸 전진
                    }
                }, matchCollectExecutor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(tasks).join();
        return savedCount.get();
    }
}
