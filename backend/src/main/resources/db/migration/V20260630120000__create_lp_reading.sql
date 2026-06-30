-- 판당 LP 증감 재설계: "LP 측정값 타임라인"을 매치 앵커에서 분리한다.
-- 기존 rank_snapshot 은 매치당 측정값 1개(앵커 최초 시점의 LP)만 박혀, league-v4 지연 시
-- 게임 전 LP 가 고정되거나 두 측정 사이에 여러 판이 끼어 거의 항상 귀속 불가였다.
-- lp_reading 은 갱신/폴링마다 측정값을 한 줄씩 쌓아(직전과 동일하면 생략) 단판 구간을 촘촘히 만든다.

CREATE TABLE lp_reading (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    puuid            VARCHAR(78)  NOT NULL,
    queue_type       VARCHAR(20)  NOT NULL,
    tier             VARCHAR(20),
    division         VARCHAR(5),
    league_points    INT          NOT NULL,
    ladder_score     INT          NOT NULL,
    read_at_epoch_ms BIGINT       NOT NULL,
    PRIMARY KEY (id)
);

-- 읽기 경로(큐별 측정값을 시간순으로 스캔)와 쓰기 경로(직전 측정값 조회) 모두 이 순서로 읽는다.
CREATE INDEX idx_lp_reading_puuid_queue_read ON lp_reading (puuid, queue_type, read_at_epoch_ms);

-- 구 모델 제거. 인덱스는 테이블 드롭 시 함께 사라진다.
DROP TABLE IF EXISTS rank_snapshot;
