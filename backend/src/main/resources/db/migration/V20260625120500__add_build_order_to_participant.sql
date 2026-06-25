-- 타임라인에서 추출한 참가자별 빌드 순서(아이템/스킬) 컬럼 추가 (Match-V5 timeline, 매치당 추가 1회 호출).
-- ddl-auto=validate 라 컬럼 추가는 Flyway 로만. 타임라인 없는 구버전 적재분은 NULL.
-- skill_build_order: 레벨업 순서를 Q/W/E/R 문자로 이어 붙인 문자열(최대 18레벨)
-- item_build_order : 구매 아이템 id 를 구매 순서대로 콤마로 이어 붙인 문자열(소모품/장신구 포함)
ALTER TABLE participant
    ADD COLUMN skill_build_order VARCHAR(32)   NULL,
    ADD COLUMN item_build_order  VARCHAR(1024) NULL;
