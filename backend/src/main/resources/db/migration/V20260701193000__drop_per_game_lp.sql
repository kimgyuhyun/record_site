-- per-game LP(판당 증감) 기능 폐기.
-- 데이터 원천 한계로 제거한다: 판당 LP 는 "게임 전/후 LP 측정값 2개의 차"로만 구할 수 있는데,
-- 측정을 시작하기 전에 이미 플레이된 게임에는 LP 이력이 존재하지 않고(과거 LP 를 주는 API 도 없음),
-- 개발용 키 1개로는 남의 계정을 op.gg 처럼 연속 기록할 수 없어 검색형 사이트에선 사실상 항상 빈값이었다.
-- LP 측정값 타임라인(lp_reading)과 폴링 추적 컬럼(summoner.tracked_until)을 함께 정리한다.

DROP TABLE IF EXISTS lp_reading;
ALTER TABLE summoner DROP COLUMN tracked_until;
