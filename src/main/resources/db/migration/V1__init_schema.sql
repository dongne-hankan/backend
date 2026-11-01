CREATE TABLE users (
    user_id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 고유 ID',
    kakao_id          VARCHAR(50)  NOT NULL COMMENT '카카오 회원번호',
    nickname          VARCHAR(12)  NOT NULL COMMENT '닉네임 2~12자, 중복 허용',
    is_admin          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '운영자 여부',
    privacy_agreed_at DATETIME     NOT NULL COMMENT '개인정보 수집·이용 동의 일시',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_kakao_id (kakao_id)
) COMMENT '회원';

CREATE TABLE refresh_token (
    token_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '토큰 ID',
    user_id     BIGINT       NOT NULL COMMENT '회원 ID',
    token_hash  VARCHAR(255) NOT NULL COMMENT '재발급 토큰 해시값',
    expires_at  DATETIME     NOT NULL COMMENT '만료 일시 (발급 후 14일)',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 일시',
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash)
) COMMENT '재발급 토큰';

CREATE TABLE region (
    region_id   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지역 ID',
    region_key  VARCHAR(50)   NOT NULL COMMENT '시드 데이터 key (예: suseong)',
    name        VARCHAR(30)   NOT NULL COMMENT '지역명 (예: 수성구)',
    province    VARCHAR(20)   NOT NULL COMMENT 'DAEGU / GYEONGBUK',
    center_lat  DECIMAL(10,7) NULL COMMENT '중심 위도',
    center_lng  DECIMAL(10,7) NULL COMMENT '중심 경도',
    PRIMARY KEY (region_id),
    UNIQUE KEY uk_region_key (region_key)
) COMMENT '지역';

CREATE TABLE experience_tag (
    tag_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '경험 태그 ID',
    tag_key     VARCHAR(50)  NOT NULL COMMENT '시드 데이터 key (예: slow-walk)',
    name        VARCHAR(30)  NOT NULL COMMENT '태그명 (예: 천천히 걷기)',
    phrase      VARCHAR(100) NOT NULL COMMENT '찾기 화면 문장 (예: 천천히 걷고 싶어요)',
    icon        VARCHAR(30)  NULL COMMENT '프론트 아이콘 키',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '표시 순서',
    PRIMARY KEY (tag_id),
    UNIQUE KEY uk_experience_tag_key (tag_key)
) COMMENT '경험 태그';

CREATE TABLE user_preference (
    user_id              BIGINT      NOT NULL COMMENT '회원 ID (PK, 회원당 1건)',
    situation            VARCHAR(10) NULL COMMENT 'STUDY / JOB / MOVE / ETC',
    home_region_id       BIGINT      NULL COMMENT '주로 지내는 지역 ID',
    last_region_id       BIGINT      NULL COMMENT '마지막 찾기 지역 ID',
    last_tag_id          BIGINT      NULL COMMENT '마지막 찾기 경험 태그 ID',
    last_duration        VARCHAR(10) NULL COMMENT '마지막 찾기 시간 MIN_30 / HOUR_1',
    onboarding_completed TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '온보딩 완료(건너뛰기 포함) 여부',
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (user_id)
) COMMENT '취향 설정';

CREATE TABLE user_favorite_tag (
    user_id  BIGINT NOT NULL COMMENT '회원 ID',
    tag_id   BIGINT NOT NULL COMMENT '좋아하는 경험 태그 ID (회원당 0~3개)',
    PRIMARY KEY (user_id, tag_id)
) COMMENT '좋아하는 경험';

CREATE TABLE place (
    place_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '장소 ID',
    place_key      VARCHAR(50)   NOT NULL COMMENT '시드 데이터 key',
    name           VARCHAR(100)  NOT NULL COMMENT '장소명',
    address        VARCHAR(255)  NOT NULL COMMENT '주소',
    lat            DECIMAL(10,7) NOT NULL COMMENT '위도 (팀이 카카오맵에서 직접 확인)',
    lng            DECIMAL(10,7) NOT NULL COMMENT '경도',
    checked_on     DATE          NOT NULL COMMENT '정보 확인일',
    time_sensitive TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '영업시간 확인 필요 여부',
    source_url     VARCHAR(500)  NULL COMMENT '공식 안내 링크',
    visible        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '공개 여부 (처음엔 비공개)',
    hidden_reason  VARCHAR(100)  NULL COMMENT '숨김 사유 (폐업 등)',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (place_id),
    UNIQUE KEY uk_place_key (place_key)
) COMMENT '장소';

CREATE TABLE place_photo (
    photo_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사진 ID',
    place_id    BIGINT       NOT NULL COMMENT '장소 ID',
    image_url   VARCHAR(500) NOT NULL COMMENT '사진 URL (팀이 직접 찍은 사진)',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '표시 순서 (0번이 대표)',
    PRIMARY KEY (photo_id)
) COMMENT '장소 사진';

CREATE TABLE resident_story (
    story_id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '주민 이야기 ID',
    story_key          VARCHAR(50)  NOT NULL COMMENT '시드 데이터 key',
    place_id           BIGINT       NOT NULL COMMENT '장소 ID',
    resident_intro     VARCHAR(100) NOT NULL COMMENT '주민 소개 (예: 범어동 20년 주민)',
    resident_name      VARCHAR(30)  NULL COMMENT '주민 실명 (WITH_NAME 이상만)',
    resident_photo_url VARCHAR(500) NULL COMMENT '주민 사진 URL (WITH_PHOTO 만)',
    reason             TEXT         NOT NULL COMMENT '좋아하는 이유',
    how_to_enjoy       TEXT         NOT NULL COMMENT '즐기는 방법',
    consent_scope      VARCHAR(20)  NOT NULL COMMENT 'INTRO_ONLY / WITH_NAME / WITH_PHOTO',
    visible            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '공개 여부',
    hidden_reason      VARCHAR(100) NULL COMMENT '숨김 사유 (동의 철회 등)',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (story_id),
    UNIQUE KEY uk_resident_story_key (story_key)
) COMMENT '주민 이야기';

CREATE TABLE course (
    course_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '코스 ID',
    course_key       VARCHAR(50)  NOT NULL COMMENT '시드 데이터 key',
    region_id        BIGINT       NOT NULL COMMENT '지역 ID (코스당 1개)',
    name             VARCHAR(100) NOT NULL COMMENT '코스명',
    intro            VARCHAR(255) NOT NULL COMMENT '한 줄 소개',
    transport        VARCHAR(20)  NOT NULL COMMENT '이동 방법 (도보, 대중교통 등)',
    thumbnail_url    VARCHAR(500) NULL COMMENT '대표 사진 URL',
    duration_minutes INT          NOT NULL COMMENT '예상 소요 시간(분), 서버 계산',
    visible          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '공개 여부',
    hidden_reason    VARCHAR(100) NULL COMMENT '숨김 사유',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (course_id),
    UNIQUE KEY uk_course_key (course_key)
) COMMENT '코스';

CREATE TABLE course_tag (
    course_id  BIGINT NOT NULL COMMENT '코스 ID',
    tag_id     BIGINT NOT NULL COMMENT '경험 태그 ID (코스당 1개 이상)',
    PRIMARY KEY (course_id, tag_id)
) COMMENT '코스 경험 태그';

CREATE TABLE course_stop (
    stop_id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '코스 장소 ID',
    course_id            BIGINT NOT NULL COMMENT '코스 ID',
    place_id             BIGINT NOT NULL COMMENT '장소 ID',
    stop_order           INT    NOT NULL COMMENT '코스 내 순서 1~4 (지도 핀 번호)',
    stay_minutes         INT    NOT NULL COMMENT '머무는 시간(분)',
    move_minutes_to_next INT    NULL COMMENT '다음 장소까지 이동 시간(분), 마지막은 NULL',
    highlight_story_id   BIGINT NULL COMMENT '대표 주민 한마디로 쓸 이야기 ID',
    PRIMARY KEY (stop_id),
    UNIQUE KEY uk_course_stop_order (course_id, stop_order)
) COMMENT '코스 장소 순서';

CREATE TABLE saved_course (
    saved_id    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '담기 ID',
    user_id     BIGINT   NOT NULL COMMENT '회원 ID',
    course_id   BIGINT   NOT NULL COMMENT '코스 ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담은 일시',
    PRIMARY KEY (saved_id),
    UNIQUE KEY uk_saved_course_user_course (user_id, course_id)
) COMMENT '담은 코스';

CREATE TABLE visit_record (
    record_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
    user_id     BIGINT       NOT NULL COMMENT '회원 ID',
    course_id   BIGINT       NOT NULL COMMENT '다녀온 코스 ID',
    feeling     VARCHAR(20)  NOT NULL COMMENT 'WANT_AGAIN / GOOD / NOT_FOR_ME',
    memo        VARCHAR(50)  NULL COMMENT '한 줄 메모 (최대 50자)',
    visited_on  DATE         NOT NULL COMMENT '다녀온 날 (오늘 이후 불가)',
    photo_url   VARCHAR(500) NULL COMMENT '기록 사진 URL (기록당 1장, P1)',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기록 일시',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
    PRIMARY KEY (record_id),
    KEY idx_visit_record_user_visited (user_id, visited_on)
) COMMENT '느낌 기록';

-- 외래키
ALTER TABLE refresh_token     ADD CONSTRAINT fk_refresh_token_user        FOREIGN KEY (user_id)            REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE user_preference   ADD CONSTRAINT fk_user_preference_user      FOREIGN KEY (user_id)            REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE user_preference   ADD CONSTRAINT fk_user_preference_home      FOREIGN KEY (home_region_id)     REFERENCES region (region_id) ON DELETE SET NULL;
ALTER TABLE user_preference   ADD CONSTRAINT fk_user_preference_last_reg  FOREIGN KEY (last_region_id)     REFERENCES region (region_id) ON DELETE SET NULL;
ALTER TABLE user_preference   ADD CONSTRAINT fk_user_preference_last_tag  FOREIGN KEY (last_tag_id)        REFERENCES experience_tag (tag_id) ON DELETE SET NULL;
ALTER TABLE user_favorite_tag ADD CONSTRAINT fk_user_favorite_tag_user    FOREIGN KEY (user_id)            REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE user_favorite_tag ADD CONSTRAINT fk_user_favorite_tag_tag     FOREIGN KEY (tag_id)             REFERENCES experience_tag (tag_id);
ALTER TABLE place_photo       ADD CONSTRAINT fk_place_photo_place         FOREIGN KEY (place_id)           REFERENCES place (place_id);
ALTER TABLE resident_story    ADD CONSTRAINT fk_resident_story_place      FOREIGN KEY (place_id)           REFERENCES place (place_id);
ALTER TABLE course            ADD CONSTRAINT fk_course_region             FOREIGN KEY (region_id)          REFERENCES region (region_id);
ALTER TABLE course_tag        ADD CONSTRAINT fk_course_tag_course         FOREIGN KEY (course_id)          REFERENCES course (course_id);
ALTER TABLE course_tag        ADD CONSTRAINT fk_course_tag_tag            FOREIGN KEY (tag_id)             REFERENCES experience_tag (tag_id);
ALTER TABLE course_stop       ADD CONSTRAINT fk_course_stop_course        FOREIGN KEY (course_id)          REFERENCES course (course_id);
ALTER TABLE course_stop       ADD CONSTRAINT fk_course_stop_place         FOREIGN KEY (place_id)           REFERENCES place (place_id);
ALTER TABLE course_stop       ADD CONSTRAINT fk_course_stop_story         FOREIGN KEY (highlight_story_id) REFERENCES resident_story (story_id) ON DELETE SET NULL;
ALTER TABLE saved_course      ADD CONSTRAINT fk_saved_course_user         FOREIGN KEY (user_id)            REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE saved_course      ADD CONSTRAINT fk_saved_course_course       FOREIGN KEY (course_id)          REFERENCES course (course_id);
ALTER TABLE visit_record      ADD CONSTRAINT fk_visit_record_user         FOREIGN KEY (user_id)            REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE visit_record      ADD CONSTRAINT fk_visit_record_course       FOREIGN KEY (course_id)          REFERENCES course (course_id);
