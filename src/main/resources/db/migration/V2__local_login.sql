ALTER TABLE users ALTER COLUMN kakao_id DROP NOT NULL; -- 카카오 회원번호 (카카오 회원만)
ALTER TABLE users ADD COLUMN login_id VARCHAR(20) NULL; -- 자체 로그인 아이디 (소문자로 저장)
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL; -- 비밀번호 해시 (카카오 회원은 없음)
ALTER TABLE users ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);
