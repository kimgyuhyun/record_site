package com.recordsite.backend.service;

import com.recordsite.backend.entity.ChampionTip;
import com.recordsite.backend.repository.ChampionTipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

// 추천/신고의 동시 요청 검증. 두 가지를 본다.
//  (1) 서로 다른 사람이 동시에 누를 때 증가분이 유실되지 않는가(Lost Update)
//  (2) 같은 사람이 동시에 연타할 때 팁당 1회로 제한되는가(중복 투표/신고)
// ⚠️ 테스트 메서드에 @Transactional 을 붙이면 모든 호출이 한 트랜잭션에 묶여 경합이 재현되지 않는다.
@SpringBootTest
class ChampionTipConcurrencyTest {

    private static final int TEST_CHAMPION_ID = 999999; // 실제 챔피언과 겹치지 않는 값

    @Autowired
    private ChampionTipService championTipService;

    @Autowired
    private ChampionTipRepository championTipRepository;

    private final List<Long> createdTipIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // 추천/신고 기록은 FK 의 ON DELETE CASCADE 로 함께 지워진다.
        createdTipIds.forEach(championTipRepository::deleteById);
        createdTipIds.clear();
    }

    @Test
    @DisplayName("서로 다른 20명이 동시에 추천해도 upvotes 는 정확히 20 이어야 한다")
    void concurrentUpvotes() throws InterruptedException {
        Long tipId = saveTip();
        int threadCount = 20;

        List<Throwable> errors = runConcurrently(threadCount,
                actorKey -> championTipService.vote(tipId, "UP", actorKey));

        assertThat(errors).isEmpty();
        assertThat(findTip(tipId).getUpvotes()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("서로 다른 20명이 동시에 비추천해도 downvotes 는 정확히 20 이어야 한다")
    void concurrentDownvotes() throws InterruptedException {
        Long tipId = saveTip();
        int threadCount = 20;

        List<Throwable> errors = runConcurrently(threadCount,
                actorKey -> championTipService.vote(tipId, "DOWN", actorKey));

        assertThat(errors).isEmpty();
        assertThat(findTip(tipId).getDownvotes()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("서로 다른 20명이 동시에 신고하면 reportCount 는 정확히 20 이고 숨김 처리된다")
    void concurrentReports() throws InterruptedException {
        Long tipId = saveTip();
        int threadCount = 20;

        List<Throwable> errors = runConcurrently(threadCount,
                actorKey -> championTipService.report(tipId, actorKey));

        assertThat(errors).isEmpty();
        ChampionTip tip = findTip(tipId);
        assertThat(tip.getReportCount()).isEqualTo(threadCount);
        assertThat(tip.isHidden()).isTrue();
    }

    @Test
    @DisplayName("서로 다른 4명의 동시 신고는 숨겨지지 않고, 5번째 사람의 신고에서 숨김이 켜진다")
    void hiddenTurnsOnAtExactlyFifthReport() throws InterruptedException {
        Long tipId = saveTip();

        runConcurrently(4, actorKey -> championTipService.report(tipId, actorKey));

        ChampionTip beforeThreshold = findTip(tipId);
        assertThat(beforeThreshold.getReportCount()).isEqualTo(4);
        assertThat(beforeThreshold.isHidden()).isFalse();

        championTipService.report(tipId, newActorKey());

        ChampionTip afterThreshold = findTip(tipId);
        assertThat(afterThreshold.getReportCount()).isEqualTo(5);
        assertThat(afterThreshold.isHidden()).isTrue();
    }

    @Test
    @DisplayName("같은 사람이 동시에 20번 추천해도 1회만 반영되고 나머지는 409 다")
    void duplicateVotesFromSameVoterCountOnce() throws InterruptedException {
        Long tipId = saveTip();
        String actorKey = newActorKey();
        int threadCount = 20;

        List<Throwable> errors = runConcurrently(threadCount,
                ignored -> championTipService.vote(tipId, "UP", actorKey));

        assertThat(findTip(tipId).getUpvotes()).isEqualTo(1);
        assertThat(errors).hasSize(threadCount - 1);
        assertThat(errors).allSatisfy(e ->
                assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("같은 사람이 임계값만큼 신고해도 1회만 반영되어 팁이 숨겨지지 않는다")
    void duplicateReportsFromSameVoterCannotHideTip() throws InterruptedException {
        Long tipId = saveTip();
        String actorKey = newActorKey();

        List<Throwable> errors = runConcurrently(20, ignored -> championTipService.report(tipId, actorKey));

        ChampionTip tip = findTip(tipId);
        assertThat(tip.getReportCount()).isEqualTo(1);
        assertThat(tip.isHidden()).isFalse();
        assertThat(errors).hasSize(19);
    }

    // 모든 스레드를 래치로 동시에 출발시켜 같은 행에 경합을 만든다. 스레드마다 새 actorKey 를 넘겨주고,
    // 같은 사람의 연타를 재현할 때는 호출부에서 이 인자를 무시하면 된다. 던져진 예외는 모아서 돌려준다.
    private List<Throwable> runConcurrently(int threadCount, Consumer<String> action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        action.accept(newActorKey());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdown();
        }
        return errors;
    }

    private String newActorKey() {
        return UUID.randomUUID().toString();
    }

    private ChampionTip findTip(Long tipId) {
        return championTipRepository.findById(tipId).orElseThrow();
    }

    private Long saveTip() {
        ChampionTip tip = championTipRepository.save(ChampionTip.of(
                TEST_CHAMPION_ID, "동시성", "동시성 테스트용 팁", "16.12", "한국어", "salt$hash"));
        createdTipIds.add(tip.getId());
        return tip.getId();
    }
}
