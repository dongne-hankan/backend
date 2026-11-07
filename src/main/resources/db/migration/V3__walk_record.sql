ALTER TABLE course ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'CURATED' COMMENT '코스 출처 CURATED / WALK';
ALTER TABLE course_stop MODIFY place_id BIGINT NULL COMMENT '장소 ID (산책 기록 코스는 NULL)';
ALTER TABLE course_stop ADD COLUMN kakao_place_id VARCHAR(30) NULL COMMENT '카카오 장소 ID (약관상 ID와 링크만 저장)';
ALTER TABLE course_stop ADD COLUMN kakao_place_url VARCHAR(500) NULL COMMENT '카카오 장소 링크';
