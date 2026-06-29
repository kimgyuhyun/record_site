-- 백그라운드 LP 폴링 추적 만료 시각.
-- 사용자가 프로필을 조회/갱신하면 미래 시점으로 연장되고, 폴러는 이 시각이 아직 미래인 소환사만 주기적으로 갱신해
-- 판당 LP 스냅샷을 촘촘히 쌓는다(op.gg식 per-game LP의 데이터 밀도 확보). 폴링 자체는 이 값을 건드리지 않는다.
ALTER TABLE summoner ADD COLUMN tracked_until DATETIME(6) NULL;

-- 최근 3일 내 랭크 갱신 이력이 있는 소환사는 곧바로 추적 대상에 포함시켜, 다음 프로필 조회 전이라도 폴링이 시작되게 한다.
UPDATE summoner
SET tracked_until = DATE_ADD(NOW(), INTERVAL 3 DAY)
WHERE rank_updated_at >= DATE_SUB(NOW(), INTERVAL 3 DAY);
