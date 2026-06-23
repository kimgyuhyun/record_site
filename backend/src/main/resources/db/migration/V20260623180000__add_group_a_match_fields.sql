-- Group A: Riot 매치 응답에 이미 들어오던 추가 필드들을 적재한다(추가 API 호출 없음).
-- ddl-auto=validate 라 컬럼 추가는 Flyway 로만. 기존 적재분은 값이 없어 0/false/NULL 로 채운다.

-- ── participant: 멀티킬/연속킬 ──
ALTER TABLE participant
    ADD COLUMN double_kills           INT     NOT NULL DEFAULT 0,
    ADD COLUMN triple_kills           INT     NOT NULL DEFAULT 0,
    ADD COLUMN quadra_kills           INT     NOT NULL DEFAULT 0,
    ADD COLUMN penta_kills            INT     NOT NULL DEFAULT 0,
    ADD COLUMN largest_multi_kill     INT     NOT NULL DEFAULT 0,
    ADD COLUMN largest_killing_spree  INT     NOT NULL DEFAULT 0,
-- 퍼스트(개인)
    ADD COLUMN first_blood_kill       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN first_tower_kill       BOOLEAN NOT NULL DEFAULT FALSE,
-- 시야/와드 상세
    ADD COLUMN wards_placed           INT     NOT NULL DEFAULT 0,
    ADD COLUMN wards_killed           INT     NOT NULL DEFAULT 0,
    ADD COLUMN vision_wards_bought    INT     NOT NULL DEFAULT 0,
    ADD COLUMN detector_wards_placed  INT     NOT NULL DEFAULT 0,
-- 힐/실드
    ADD COLUMN total_heal                          BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_heals_on_teammates            BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_damage_shielded_on_teammates  BIGINT NOT NULL DEFAULT 0,
-- CC
    ADD COLUMN time_ccing_others      INT     NOT NULL DEFAULT 0,
    ADD COLUMN total_time_cc_dealt    INT     NOT NULL DEFAULT 0,
-- 골드/생존
    ADD COLUMN gold_spent                 INT NOT NULL DEFAULT 0,
    ADD COLUMN longest_time_spent_living  INT NOT NULL DEFAULT 0,
    ADD COLUMN total_time_spent_dead      INT NOT NULL DEFAULT 0,
-- 스킬/스펠 사용 횟수
    ADD COLUMN spell1_casts           INT     NOT NULL DEFAULT 0,
    ADD COLUMN spell2_casts           INT     NOT NULL DEFAULT 0,
    ADD COLUMN spell3_casts           INT     NOT NULL DEFAULT 0,
    ADD COLUMN spell4_casts           INT     NOT NULL DEFAULT 0,
-- Riot 파생 지표(challenges) — 모드/구버전에 따라 없을 수 있어 NULL 허용
    ADD COLUMN kda                       DOUBLE NULL,
    ADD COLUMN kill_participation        DOUBLE NULL,
    ADD COLUMN team_damage_percentage    DOUBLE NULL,
    ADD COLUMN damage_per_minute         DOUBLE NULL,
    ADD COLUMN gold_per_minute           DOUBLE NULL,
    ADD COLUMN vision_score_per_minute   DOUBLE NULL,
    ADD COLUMN solo_kills                INT    NULL,
-- 룬 페이지 전체(나머지 룬) — 룬 없는 모드는 NULL
    ADD COLUMN primary_rune1  INT NULL,
    ADD COLUMN primary_rune2  INT NULL,
    ADD COLUMN primary_rune3  INT NULL,
    ADD COLUMN sub_rune1      INT NULL,
    ADD COLUMN sub_rune2      INT NULL;

-- ── matches: 패치 버전 + 팀별 퍼스트 오브젝트 플래그 ──
ALTER TABLE matches
    ADD COLUMN game_version VARCHAR(40) NULL,
    ADD COLUMN blue_first_blood        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_first_tower        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_first_dragon       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_first_baron        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_first_rift_herald  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN blue_first_inhibitor    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_blood         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_tower         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_dragon        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_baron         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_rift_herald   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN red_first_inhibitor     BOOLEAN NOT NULL DEFAULT FALSE;
