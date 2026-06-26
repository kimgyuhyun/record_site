package com.recordsite.backend.service;

import com.recordsite.backend.dto.RefreshJobDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 전적 갱신 작업의 큐·중복 락·진행 상태를 Redis 로 관리한다.
 *
 * 키 구성:
 *  - refresh:lock:{puuid}  진행 중(및 직후 쿨다운) 표시. 값은 jobId. EX 로 중복 갱신 차단 + 워커 사망 시 자동 해제.
 *  - refresh:queue         대기 jobId 리스트. LPUSH 로 넣고 워커가 BRPOP 으로 꺼낸다.
 *  - job:{jobId}           status/total/done/puuid 해시. TTL 로 폴링 종료 후 자연 소멸.
 *
 * Riot 호출·DB 저장은 하지 않는다(수집은 MatchService 담당). 여기는 "작업의 상태"만 다룬다.
 */
@Slf4j
@Service
public class RefreshJobStore {

    public static final String QUEUE_KEY = "refresh:queue";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    // 락 값이 내 jobId 와 같을 때만 삭제(다른 갱신의 락을 건드리지 않는 compare-and-delete).
    private static final DefaultRedisScript<Long> RELEASE_IF_MINE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final Duration lockTtl;
    private final Duration jobTtl;

    public RefreshJobStore(
            StringRedisTemplate redis,
            @Value("${riot.refresh.lock-ttl-seconds:120}") long lockTtlSeconds,
            @Value("${riot.refresh.job-ttl-seconds:600}") long jobTtlSeconds) {
        this.redis = redis;
        this.lockTtl = Duration.ofSeconds(lockTtlSeconds);
        this.jobTtl = Duration.ofSeconds(jobTtlSeconds);
    }

    // 갱신 요청 접수: 락 선점에 성공하면 새 잡을 만들어 큐에 넣고, 이미 진행 중(쿨다운 포함)이면 그 잡 상태를 그대로 돌려준다.
    public RefreshJobDto submit(String puuid) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey(puuid), jobId, lockTtl);

        if (Boolean.TRUE.equals(acquired)) {
            redis.opsForHash().putAll(jobKey(jobId), Map.of(
                    "status", STATUS_PENDING, "total", "0", "done", "0", "puuid", puuid));
            redis.expire(jobKey(jobId), jobTtl);
            redis.opsForList().leftPush(QUEUE_KEY, jobId);   // LPUSH → 워커가 BRPOP(우측)으로 소비
            return new RefreshJobDto(jobId, STATUS_PENDING, 0, 0);
        }

        // 이미 갱신 중(또는 쿨다운): 락이 가리키는 기존 jobId 의 상태를 반환
        String runningJobId = redis.opsForValue().get(lockKey(puuid));
        RefreshJobDto running = runningJobId == null ? null : find(runningJobId);
        if (running != null) {
            return running;
        }
        // 락은 살아 있는데 잡 해시만 만료된 드문 경합: 진행 중으로 간주해 합성 응답
        return new RefreshJobDto(runningJobId == null ? jobId : runningJobId, STATUS_PROCESSING, 0, 0);
    }

    public RefreshJobDto find(String jobId) {
        Map<Object, Object> hash = redis.opsForHash().entries(jobKey(jobId));
        if (hash.isEmpty()) {
            return null;
        }
        return new RefreshJobDto(
                jobId,
                String.valueOf(hash.getOrDefault("status", STATUS_PENDING)),
                parseInt(hash.get("total")),
                parseInt(hash.get("done")));
    }

    public String puuidOf(String jobId) {
        Object puuid = redis.opsForHash().get(jobKey(jobId), "puuid");
        return puuid == null ? null : puuid.toString();
    }

    public void markProcessing(String jobId) {
        redis.opsForHash().put(jobKey(jobId), "status", STATUS_PROCESSING);
    }

    public void setTotal(String jobId, int total) {
        redis.opsForHash().put(jobKey(jobId), "total", String.valueOf(total));
    }

    public void incrementDone(String jobId) {
        redis.opsForHash().increment(jobKey(jobId), "done", 1);
    }

    public void markDone(String jobId) {
        redis.opsForHash().put(jobKey(jobId), "status", STATUS_DONE);
    }

    public void markFailed(String jobId) {
        redis.opsForHash().put(jobKey(jobId), "status", STATUS_FAILED);
    }

    public void releaseLock(String puuid, String jobId) {
        redis.execute(RELEASE_IF_MINE, List.of(lockKey(puuid)), jobId);
    }

    private int parseInt(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String lockKey(String puuid) {
        return "refresh:lock:" + puuid;
    }

    private String jobKey(String jobId) {
        return "job:" + jobId;
    }
}
