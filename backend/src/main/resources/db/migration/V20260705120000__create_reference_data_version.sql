-- DDragon 정적 메타데이터(아이템 등)가 어느 패치 버전으로 시딩됐는지 기록한다.
-- 앱 기동 시 이 값과 코드의 목표 버전(DataDragonService.VERSION)을 비교해, 다르면 해당 리소스를 재적재한다.
CREATE TABLE reference_data_version (
    resource   VARCHAR(30) NOT NULL,
    version    VARCHAR(20) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (resource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
