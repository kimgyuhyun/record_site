package com.recordsite.backend.service;

import com.recordsite.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 백그라운드 LP 폴러 — 추적 중인 소환사의 전적 갱신을 주기적으로 큐잉한다.
 *
 * 왜 필요한가: 판당 LP 증감은 "한 게임을 사이에 둔 두 LP 스냅샷의 차이"로만 정확히 구할 수 있다.
 * 수동 갱신만으로는 스냅샷이 드물어 한 구간에 여러 판이 섞이고, 그러면 어느 판 몫인지 쪼갤 수 없어 표시되지 않는다.
 * 게임 1판(~30분)보다 짧은 주기로 폴링하면 매 랭크 게임 직후 스냅샷이 한 번씩 찍혀 단일 게임 구간이 생기고,
 * 그제야 판당 LP가 op.gg 처럼 채워진다.
 *
 * 실제 갱신 작업(매치 수집 + LP 갱신 + 스냅샷 기록)은 큐에 넣기만 하면 RefreshJobWorker 가 수행한다.
 * RefreshJobStore.submit 의 락(TTL=쿨다운)이 중복·동시 갱신을 막으므로, 유저 수동 갱신과 겹쳐도 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankPollingScheduler {

    private final SummonerRepository summonerRepository;
    private final RefreshJobStore refreshJobStore;

    @Value("${riot.rank-poll.enabled:true}")
    private boolean enabled;

    // 한 주기에 큐잉할 최대 추적 소환사 수. 레이트리밋과 유저 갱신 응답성을 위해 작게 유지한다.
    @Value("${riot.rank-poll.batch-size:20}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${riot.rank-poll.interval-ms:600000}",
            initialDelayString = "${riot.rank-poll.initial-delay-ms:90000}")
    public void pollTrackedSummoners() {
        if (!enabled) {
            return;
        }

        List<String> puuids = summonerRepository.findTrackedPuuids(
                LocalDateTime.now(), PageRequest.of(0, batchSize));
        if (puuids.isEmpty()) {
            return;
        }

        for (String puuid : puuids) {
            refreshJobStore.submit(puuid); // 락/쿨다운이 중복·연타를 차단한다. 워커가 갱신+스냅샷을 수행.
        }
        log.info("LP 폴링: 추적 소환사 {}명 갱신 큐잉", puuids.size());
    }
}
