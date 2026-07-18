-- 팁 추천/비추천/신고의 1인 1회 제약을 DB 레벨에서 보장한다.
-- 이전에는 아무 검증이 없어 신고를 5회 반복 호출하면 임의의 글을 숨길 수 있었다.
-- actor_key 는 클라이언트 IP 를 솔트와 함께 해시한 값(원본 IP 는 저장하지 않는다).
CREATE TABLE champion_tip_interaction (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tip_id           BIGINT       NOT NULL,
    actor_key        VARCHAR(64)  NOT NULL,
    interaction_type VARCHAR(20)  NOT NULL,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    -- 동시 요청으로 중복이 비집고 들어오는 것까지 막는 최종 방어선(애플리케이션 검사만으로는 경합에 진다).
    CONSTRAINT uk_champion_tip_interaction UNIQUE (tip_id, actor_key, interaction_type),
    -- 팁이 지워지면 상호작용 이력도 함께 사라진다.
    CONSTRAINT fk_champion_tip_interaction_tip FOREIGN KEY (tip_id)
        REFERENCES champion_tip (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
