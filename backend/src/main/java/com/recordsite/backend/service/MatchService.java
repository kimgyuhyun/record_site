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

import java.util.List;
import java.util.Set;

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
    private final RankSnapshotService rankSnapshotService;
    private final SummonerCrawlService summonerCrawlService;


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

        // 라이엇 API 에서 최근 매치 20개 조회
        List<String> matchIdList = riotMatchClient.getMatchIdsByPuuid(puuid, 0, 20);
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

        // db에 없는 매치 저장
        int newCount = 0;
        for (String matchId : newMatchIds) {
                try {
                    matchSaveHelper.saveMatchWithParticipants(matchId, puuid);
                    newCount++;
                } catch (Exception e) {
                    log.warn("매치 저장 실패, 스킵: matchId={}, error={}", matchId, e.getMessage());
                } finally {
                    progress.onMatchDone(); // 성공·스킵 무관하게 진행률 한 칸 전진
                }
        }

        // 랭크 갱신 + 스냅샷은 신규 매치 유무와 무관하게 매 갱신마다 수행한다.
        //  - 판당 LP 증감은 "최신 LP 리딩" 2개의 차이로 계산되므로, 리딩을 자주 확보할수록 표본이 쌓인다.
        //  - 특히 크롤러가 동료 경로로 내 매치를 먼저 저장한 경우, 내가 직접 갱신해도 신규 매치가 0이 되어
        //    예전엔 스냅샷이 안 찍혔다 → 이제는 그래도 현재 LP를 최신 랭크 매치에 앵커로 박는다.
        leagueService.updateAndSaveLeague(puuid);
        rankSnapshotService.recordSnapshots(puuid);

        // 이번에 새로 만난 동료 puuid 들만 크롤러 큐에 적재 → 백그라운드로 매치망 점진 확장(챔피언 통계 표본 확보)
        if (!newMatchIds.isEmpty()) {
            summonerCrawlService.enqueueNeighborsFromMatches(newMatchIds, 1, puuid);
        }

        return newCount;
    }
}
