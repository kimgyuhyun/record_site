-- 챔피언별 운영 팁 게시판(코멘트). 로그인 없이 닉네임으로 한 줄 팁을 남기고 추천/비추천·신고가 달린다.
CREATE TABLE champion_tip (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    champion_id   INT          NOT NULL,
    nickname      VARCHAR(20)  NOT NULL,
    content       VARCHAR(500) NOT NULL,
    patch_version VARCHAR(20),
    language      VARCHAR(20)  NOT NULL,
    upvotes       INT          NOT NULL,
    downvotes     INT          NOT NULL,
    report_count  INT          NOT NULL,
    hidden        BIT(1)       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_champion_tip_champion (champion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
