CREATE TABLE match_ban (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    champion_id INT NOT NULL,
    team_id INT NOT NULL,
    pick_turn INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_ban_match FOREIGN KEY (match_id) REFERENCES matches (id),
    INDEX idx_match_ban_champion (champion_id)
);
