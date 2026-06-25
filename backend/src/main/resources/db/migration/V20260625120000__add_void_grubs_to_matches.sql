-- 팀별 공허 유충(void grub / horde) 처치 수 추가 (Match-V5 objectives.horde, 추가 API 호출 없음).
-- ddl-auto=validate 라 컬럼 추가는 Flyway 로만. 기존 적재분은 값이 없어 0 으로 채운다.
ALTER TABLE matches
    ADD COLUMN blue_horde_kills INT NOT NULL DEFAULT 0,
    ADD COLUMN red_horde_kills  INT NOT NULL DEFAULT 0;
