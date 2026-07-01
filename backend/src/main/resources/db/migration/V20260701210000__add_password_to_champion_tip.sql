-- 비로그인 팁의 본인 삭제를 위해 작성 비밀번호 해시를 저장한다(PBKDF2 "salt$hash").
-- champion_tip 은 방금 추가된 빈 테이블이라 NOT NULL 로 바로 추가해도 안전하다.
ALTER TABLE champion_tip ADD COLUMN password_hash VARCHAR(200) NOT NULL;
