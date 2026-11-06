ALTER TABLE users MODIFY kakao_id VARCHAR(50) NULL COMMENT '카카오 회원번호 (카카오 회원만)';
ALTER TABLE users ADD COLUMN login_id VARCHAR(20) NULL COMMENT '자체 로그인 아이디 (소문자로 저장)' AFTER kakao_id;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '비밀번호 해시 (카카오 회원은 없음)' AFTER login_id;
ALTER TABLE users ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);
