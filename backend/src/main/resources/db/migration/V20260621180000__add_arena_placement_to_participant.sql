-- 아레나(CHERRY) 등수 표시용 컬럼. 협곡 등 다른 모드 매치에서는 NULL로 남는다.
ALTER TABLE participant
    ADD COLUMN placement         INT NULL,
    ADD COLUMN subteam_placement INT NULL,
    ADD COLUMN player_subteam_id INT NULL;
