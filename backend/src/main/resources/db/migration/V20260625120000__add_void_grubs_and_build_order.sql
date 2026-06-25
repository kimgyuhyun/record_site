-- 공허 유충(void grub / horde) 처치 수 + 타임라인 추출 빌드 순서(아이템/스킬) 컬럼 추가.
-- ddl-auto=validate 라 컬럼 추가는 Flyway 로만. 기존 적재분은 값이 없어 0/NULL 로 채운다.

-- ── matches: 팀별 공허 유충 처치 수 (Match-V5 objectives.horde, 추가 API 호출 없음) ──
ALTER TABLE matches
    ADD COLUMN blue_horde_kills INT NOT NULL DEFAULT 0,
    ADD COLUMN red_horde_kills  INT NOT NULL DEFAULT 0;

-- ── participant: 타임라인 추출 빌드 순서 (Match-V5 timeline, 매치당 추가 1회 호출) ──
-- skill_build_order: 레벨업 순서를 Q/W/E/R 문자로 이어 붙인 문자열(최대 18레벨) — 타임라인 없으면 NULL
-- item_build_order : "아이템id:구매초" 를 구매 순서대로 콤마로 이어 붙인 문자열 — 타임라인 없으면 NULL
ALTER TABLE participant
    ADD COLUMN skill_build_order VARCHAR(32)   NULL,
    ADD COLUMN item_build_order  VARCHAR(1024) NULL;
