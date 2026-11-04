ALTER TABLE users MODIFY kakao_id VARCHAR(50) NULL COMMENT '카카오 회원번호 (연동한 회원만)';
ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL COMMENT '로그인 이메일 (소문자로 저장)' AFTER kakao_id;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '비밀번호 해시 (카카오 전용 회원은 없음)' AFTER email;
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE TABLE password_reset_token (
    token_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '토큰 ID',
    user_id     BIGINT       NOT NULL COMMENT '회원 ID',
    token_hash  VARCHAR(255) NOT NULL COMMENT '재설정 토큰 해시값',
    expires_at  DATETIME     NOT NULL COMMENT '만료 일시 (발급 후 30분)',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 일시',
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_password_reset_token_hash (token_hash)
) COMMENT '비밀번호 재설정 토큰';
