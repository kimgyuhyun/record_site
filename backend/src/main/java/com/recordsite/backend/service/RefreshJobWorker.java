package com.recordsite.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 전적 갱신 작업 큐(refresh:queue)의 단일 소비자.
 *
 * 전용 스레드 하나가 BRPOP 으로 잡을 기다렸다가, 꺼내면 그 puuid 의 최근 매치를 수집한다.
 * 실제 수집·증분·레이트리밋·부분실패 처리는 기존 MatchService 를 그대로 재사용하고,
 * 여기서는 잡 상태(PROCESSING/DONE/FAILED)와 진행률만 RefreshJobStore 로 갱신한다.
 *
 * 큐가 비면 BRPOP 이 블록(타임아웃까지 잠듦)하므로, 폴링과 달리 idle CPU 를 쓰지 않고
 * 작업이 들어오는 즉시 깨어나 처리한다. 종료는 SmartLifecycle.stop() 에서 인터럽트로 루프를 깨워 정리한다.
 */
@Slf4j
@Component
public class RefreshJobWorker implements SmartLifecycle {

    private final StringRedisTemplate redis;
    private final RefreshJobStore jobStore;
    private final MatchService matchService;
    private final boolean enabled;
    private final Duration blockTimeout;

    private volatile boolean running = false;
    private Thread worker;

    public RefreshJobWorker(
            StringRedisTemplate redis,
            RefreshJobStore jobStore,
            MatchService matchService,
            @Value("${riot.refresh.worker-enabled:true}") boolean enabled,
            @Value("${riot.refresh.block-timeout-seconds:5}") long blockTimeoutSeconds) {
        this.redis = redis;
        this.jobStore = jobStore;
        this.matchService = matchService;
        this.enabled = enabled;
        this.blockTimeout = Duration.ofSeconds(blockTimeoutSeconds);
    }

    @Override
    public void start() {
        if (!enabled || running) {
            return;
        }
        running = true;
        worker = new Thread(this::consumeLoop, "refresh-job-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("전적 갱신 워커 시작");
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt(); // BRPOP 대기 중인 스레드를 깨워 루프를 빠져나오게 한다
        }
        log.info("전적 갱신 워커 종료 요청");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void consumeLoop() {
        while (running) {
            try {
                String jobId = redis.opsForList().rightPop(RefreshJobStore.QUEUE_KEY, blockTimeout); // BRPOP
                if (jobId != null) {
                    process(jobId);
                }
            } catch (Exception e) {
                if (!running) {
                    break; // 종료 중 인터럽트로 인한 예외는 정상 종료 흐름
                }
                log.warn("갱신 워커 루프 오류, 잠시 후 계속: {}", e.getMessage());
                sleepQuietly();
            }
        }
    }

    private void process(String jobId) {
        String puuid = jobStore.puuidOf(jobId);
        if (puuid == null) {
            log.warn("잡 정보 없음(만료?), 스킵: jobId={}", jobId);
            return;
        }

        jobStore.markProcessing(jobId);
        try {
            matchService.refreshMatchesByPuuid(puuid, new RefreshProgress() {
                @Override public void onTotal(int total) { jobStore.setTotal(jobId, total); }
                @Override public void onMatchDone() { jobStore.incrementDone(jobId); }
            });
            jobStore.markDone(jobId);
            // 성공 시 락은 일부러 남겨 둔다 → TTL(쿨다운) 동안 연타 갱신을 막는다.
        } catch (Exception e) {
            jobStore.markFailed(jobId);
            jobStore.releaseLock(puuid, jobId); // 실패는 즉시 재시도 가능하도록 락 해제
            log.warn("전적 갱신 실패: jobId={}, error={}", jobId, e.getMessage());
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(1_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
