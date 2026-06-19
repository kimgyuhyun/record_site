package com.recordsite.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Riot 개발용 키 레이트리밋(기본 20 req/s, 100 req/2분) 보호용 슬라이딩 윈도우 리미터.
 *
 * 유저 검색 트래픽과 백그라운드 크롤러가 같은 빈을 공유하므로 전체 호출 예산을 함께 지킨다.
 * 모든 Riot API 호출은 acquire() 를 통과해야 하며, 예산이 없으면 슬롯이 빌 때까지 블록한다.
 * wait() 로 대기해 모니터를 놓아주므로, 대기 중에도 다른 스레드가 예산을 확인할 수 있다.
 */
@Component
public class RiotApiRateLimiter {

    private static final long ONE_SECOND_MS = 1_000L;
    private static final long TWO_MINUTES_MS = 120_000L;

    private final int perSecondLimit;
    private final int perTwoMinuteLimit;

    // 최근 호출 시각(ms). head = 가장 오래된 호출.
    private final Deque<Long> callTimestamps = new ArrayDeque<>();

    public RiotApiRateLimiter(
            @Value("${riot.api.rate-limit.requests-per-second:18}") int perSecondLimit,
            @Value("${riot.api.rate-limit.requests-per-two-minutes:95}") int perTwoMinuteLimit) {
        this.perSecondLimit = perSecondLimit;
        this.perTwoMinuteLimit = perTwoMinuteLimit;
    }

    // 호출 예산이 생길 때까지 블록한 뒤 현재 호출을 기록한다.
    public synchronized void acquire() {
        while (true) {
            long now = System.currentTimeMillis();
            evictOlderThan(now - TWO_MINUTES_MS);

            long inLastSecond = countSince(now - ONE_SECOND_MS);
            int inLastTwoMinutes = callTimestamps.size();

            boolean secondLimited = inLastSecond >= perSecondLimit;
            boolean twoMinuteLimited = inLastTwoMinutes >= perTwoMinuteLimit;
            if (!secondLimited && !twoMinuteLimited) {
                callTimestamps.addLast(now);
                return;
            }

            awaitFreeSlot(computeWaitMs(now, secondLimited));
        }
    }

    private void evictOlderThan(long cutoff) {
        while (!callTimestamps.isEmpty() && callTimestamps.peekFirst() < cutoff) {
            callTimestamps.pollFirst();
        }
    }

    private long countSince(long cutoff) {
        long count = 0;
        Iterator<Long> newestFirst = callTimestamps.descendingIterator();
        while (newestFirst.hasNext() && newestFirst.next() >= cutoff) {
            count++;
        }
        return count;
    }

    // 초당 한도면 짧게 재확인, 2분 한도면 가장 오래된 호출이 윈도우를 벗어날 때까지 대기.
    private long computeWaitMs(long now, boolean secondLimited) {
        if (secondLimited) {
            return 50L;
        }
        Long oldest = callTimestamps.peekFirst();
        if (oldest == null) {
            return 50L;
        }
        return Math.max(50L, (oldest + TWO_MINUTES_MS) - now);
    }

    private void awaitFreeSlot(long waitMs) {
        try {
            wait(waitMs); // 모니터를 놓고 대기 → 타임아웃 후 깨어나 다시 예산 확인
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Riot 레이트리밋 대기 중 인터럽트", e);
        }
    }
}
