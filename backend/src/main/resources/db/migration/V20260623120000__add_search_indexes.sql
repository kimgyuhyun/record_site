-- 검색/조회 핫패스 인덱스. ddl-auto=validate 이므로 인덱스 추가는 Flyway 로만 한다.
-- (FK 컬럼 match_id/summoner_id 는 MySQL 이 FK 인덱스를 자동 생성하지만, 아래 컬럼들은 비유니크/비FK 라 미인덱싱 상태)

-- 매치 목록·소환사 통계의 최상위 진입점. 거의 모든 조회가 participant.puuid 로 필터된다
-- (findMatchRecordByPuuid, aggregatePlayedChampions, findChampionPickCounts, findMatchIdsByPuuidAndQueueId ...).
-- participant 테이블은 매치당 10~16행으로 가장 빨리 커지므로, 이 인덱스가 없으면 매번 풀스캔이 된다.
CREATE INDEX idx_participant_puuid ON participant (puuid);

-- 소환사 이름 검색(findByNameAndTagLine / findAllByName)의 동등검색 가속.
-- 선두 컬럼 name 으로 name 단독·name+tag_line 둘 다 커버한다.
-- (findByNameContainingIgnoreCase 의 %LIKE% 는 선두 와일드카드라 인덱스 비대상 — 정확검색만 가속됨)
CREATE INDEX idx_summoner_name_tag ON summoner (name, tag_line);

-- 큐 필터 집계(aggregatePlayedChampions/aggregateChampionStatsByPosition 등 queueId 조건)에서
-- matches.queue_id 필터 가속.
CREATE INDEX idx_matches_queue_id ON matches (queue_id);

-- 프로필 진입 시 LP 타임라인 계산(findByPuuidOrderByCreatedAtAsc).
-- puuid 로 묶어 created_at 순으로 바로 읽도록 복합 인덱스.
CREATE INDEX idx_rank_snapshot_puuid_created ON rank_snapshot (puuid, created_at);
