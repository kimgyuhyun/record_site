package com.recordsite.backend.service;

import com.recordsite.backend.entity.CrawlStatus;
import com.recordsite.backend.entity.Participant;
import com.recordsite.backend.entity.PendingSummonerRefresh;
import com.recordsite.backend.repository.MatchRepository;
import com.recordsite.backend.repository.ParticipantRepository;
import com.recordsite.backend.repository.PendingSummonerRefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 매치망 점진 수집 크롤러.
 *
 * 유저가 검색하면 그 매치의 동료 puuid 들이 작업 큐(pending_summoner_refresh)에 적재되고,
 * 스케줄러가 한 건씩 꺼내 그 puuid 의 최근 매치를 수집한다. depth 가 남아 있으면 새로 만난
 * 동료를 다시 큐에 넣어 BFS 형태로 매치망이 퍼진다 — 챔피언 통계의 표본을 자연스럽게 불린다.
 *
 * 모든 Riot 호출은 RiotApiClientInterceptor 의 공유 레이트리밋을 통과하므로, 처리 간격과 무관하게
 * 개발 키 한도를 넘지 않는다(예산이 없으면 호출이 블록될 뿐 429 가 나지 않음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerCrawlService {

    private final RiotMatchClient riotMatchClient;
    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final MatchSaveHelper matchSaveHelper;
    private final PendingSummonerRefreshRepository queueRepository;

    @Value("${riot.crawler.enabled:true}")
    private boolean enabled;

    @Value("${riot.crawler.max-depth:1}")
    private int maxDepth;

    @Value("${riot.crawler.daily-limit:300}")
    private int dailyLimit;

    @Value("${riot.crawler.matches-per-puuid:10}")
    private int matchesPerPuuid;

    // 큐에서 미처리 puuid 1건을 꺼내 매치를 수집하고, depth 가 남으면 동료 puuid 를 큐에 추가한다.
    // 스프링 기본 스케줄러는 단일 스레드라 동시 처리가 없으므로 별도 클레임 락을 두지 않는다.
    @Scheduled(fixedDelayString = "${riot.crawler.interval-ms:20000}")
    public void processNext() {
        if (!enabled) {
            return;
        }
        if (reachedDailyLimit()) {
            return;
        }

        PendingSummonerRefresh job = queueRepository
                .findFirstByStatusOrderByIdAsc(CrawlStatus.PENDING)
                .orElse(null);
        if (job == null) {
            return;
        }

        try {
            List<String> newMatchIds = collectMatches(job.getPuuid());
            if (job.getDepth() < maxDepth) {
                enqueueNeighborsFromMatches(newMatchIds, job.getDepth() + 1, job.getPuuid());
            }
            job.markDone();
            log.info("크롤 완료: puuid={}, depth={}, 신규매치={}",
                    mask(job.getPuuid()), job.getDepth(), newMatchIds.size());
        } catch (Exception e) {
            job.markFailed();
            log.warn("크롤 실패: puuid={}, error={}", mask(job.getPuuid()), e.getMessage());
        }
        queueRepository.save(job);
    }

    private boolean reachedDailyLimit() {
        long processedToday = queueRepository.countByProcessedAtAfter(LocalDate.now().atStartOfDay());
        return processedToday >= dailyLimit;
    }

    // puuid 의 최근 매치를 받아 DB에 없는 것만 저장하고, 새로 저장한 matchId 목록을 반환한다.
    private List<String> collectMatches(String puuid) {
        List<String> matchIds = riotMatchClient.getMatchIdsByPuuid(puuid, 0, matchesPerPuuid);
        if (matchIds.isEmpty()) {
            return List.of();
        }

        Set<String> existingIds = matchRepository.findExistingMatchIds(matchIds);
        List<String> newMatchIds = matchIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        for (String matchId : newMatchIds) {
            try {
                matchSaveHelper.saveMatchWithParticipants(matchId, puuid);
            } catch (Exception e) {
                log.warn("크롤 매치 저장 실패, 스킵: matchId={}, error={}", matchId, e.getMessage());
            }
        }
        return newMatchIds;
    }

    // 주어진 매치들의 참가자 puuid(originPuuid 제외)를 큐에 PENDING 으로 추가한다(이미 있으면 스킵).
    // 검색 시 시딩(MatchService)과 크롤러 확장(processNext)이 함께 사용한다.
    public void enqueueNeighborsFromMatches(Collection<String> matchIds, int depth, String originPuuid) {
        if (!enabled) {
            return; // 크롤러 비활성화 시 큐에 적재하지 않는다(소비자 processNext 도 같은 플래그로 멈춘다).
        }
        if (matchIds.isEmpty()) {
            return;
        }

        Set<String> neighborPuuids = participantRepository.findByMatch_MatchIdIn(List.copyOf(matchIds)).stream()
                .map(Participant::getPuuid)
                .filter(puuid -> puuid != null && !puuid.equals(originPuuid))
                .collect(Collectors.toSet());

        for (String puuid : neighborPuuids) {
            if (!queueRepository.existsByPuuid(puuid)) {
                queueRepository.save(PendingSummonerRefresh.pending(puuid, depth));
            }
        }
    }

    // 로그에 puuid 전체를 남기지 않도록 앞 8자만 노출한다.
    private String mask(String puuid) {
        if (puuid == null) {
            return "null";
        }
        return puuid.substring(0, Math.min(8, puuid.length())) + "…";
    }
}
