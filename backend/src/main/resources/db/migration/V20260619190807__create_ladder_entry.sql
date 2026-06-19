CREATE TABLE ladder_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    queue_type VARCHAR(20) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    rank_position INT NOT NULL,
    puuid VARCHAR(78) NOT NULL,
    game_name VARCHAR(255) NULL,
    tag_line VARCHAR(255) NULL,
    league_points INT NOT NULL,
    wins INT NOT NULL,
    losses INT NOT NULL,
    refreshed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ladder_queue_rank UNIQUE (queue_type, rank_position)
);
