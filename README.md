# 동네 한 칸 Backend

대구·경북에 새로 온 사람에게 주민의 일상을 코스로 잇는 서비스 "동네 한 칸"의 API 서버입니다.

## 기술 스택

Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Flyway · MySQL 8 · Gradle

## 시작하기

```bash
./gradlew bootRun                                         # H2와 샘플 데이터로 실행 (local 프로필)
SPRING_PROFILES_ACTIVE=default ./gradlew bootRun          # MySQL로 실행 (docker compose up -d 또는 Homebrew mysql@8.4)
./gradlew test                                            # H2로 실행
```

스키마는 앱을 켤 때 Flyway가 `src/main/resources/db/migration`을 적용해 만듭니다. `db/dev`의 샘플 데이터는 local·test 프로필에만 들어가며, 주민 이야기는 가상 예시이고 좌표는 확인되지 않은 값입니다. 실행 전에 `.env.example`을 `.env`로 복사해 값을 채우세요. `.env`는 앱을 켤 때 자동으로 읽습니다.

비밀번호 재설정 메일은 `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`으로 SMTP를 설정하면 발송합니다. `MAIL_HOST`가 비어 있으면 메일 대신 재설정 링크를 로그에 남깁니다(로컬 개발용). 링크 주소는 `PASSWORD_RESET_URL`(기본 `http://localhost:5173/password-reset`)입니다.

## 폴더 구조

```text
src/main/java/com/sixteez/dongnehankan
├── global     공통 설정(config), 예외(exception), 응답 형식(response)
├── auth       카카오 로그인, 회원가입, 토큰
├── user       내 정보, 취향 설정
├── my         담은 코스, 느낌 기록, 나의 동네 한 칸
├── find       찾기 선택지, 추천
├── course     코스, 장소, 주민 이야기, 지역, 경험 태그
└── admin      콘텐츠 관리, 시드 데이터
```

도메인은 API명세서의 분류(auth, user, find, course, my, admin)와 맞췄습니다. 각 도메인 안은 `controller`, `service`, `dto`, `entity`, `repository`로 나눕니다. 빈 폴더를 유지하는 `.gitkeep`은 파일을 추가할 때 지워도 됩니다.

## API 규칙

- 경로는 `/api`로 시작합니다.
- 인증은 `Authorization: Bearer {accessToken}` 헤더를 씁니다.
- 성공 응답은 `{ "data": ... }` 형식이고, `ApiResponse.of(...)`로 만듭니다.
- 목록 응답의 `data`는 `{ items, page, size, totalElements, hasNext }` 형식이고, `PageResponse.of(...)`로 만듭니다. `page`는 0부터 시작합니다.
- 실패 응답은 `{ "code": "...", "message": "...", "errors": [{ "field", "message" }] }` 형식입니다. 코드는 `ErrorCode`에 추가하고 `BusinessException`으로 던집니다.

## DB 변경

적용된 마이그레이션 파일은 고치지 않습니다. 스키마를 바꿀 때는 `V2__설명.sql`처럼 새 파일을 추가합니다. 엔티티와 스키마가 다르면 `ddl-auto: validate`가 앱 시작을 막습니다.

## 남은 일

- 온보딩 취향 설정, 조건 기억, 관리자 API (시드 데이터 입력 포함)
- 회원 탈퇴 때 카카오 연결 끊기 (Admin 키 필요)

## 문서

기획 문서(PRD, 기능명세서, ERD, API명세서)는 팀 노션에 있습니다.
