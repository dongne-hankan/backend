# 동네 한 칸 Backend

대구·경북에 새로 온 사람에게 주민의 일상을 코스로 잇는 서비스 "동네 한 칸"의 API 서버입니다.

## 기술 스택

Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Flyway · MySQL 8 · Gradle

## 시작하기

```bash
docker compose up -d   # 로컬 MySQL 실행
./gradlew bootRun      # http://localhost:8080
./gradlew test         # MySQL 없이 H2로 실행
```

스키마는 앱을 켤 때 Flyway가 `src/main/resources/db/migration`을 적용해 만듭니다. 환경 변수 목록은 `.env.example`을 참고하세요.

## 폴더 구조

```text
src/main/java/com/sixteez/dongnehankan
├── global     공통 설정(config), 예외(exception), 응답 형식(response)
├── auth       카카오 로그인, 회원가입, 토큰 (Backend A)
├── user       내 정보, 취향 설정 (Backend A)
├── my         담은 코스, 느낌 기록, 나의 동네 한 칸 (Backend A)
├── find       찾기 선택지, 추천 (Backend B)
├── course     코스, 장소, 주민 이야기, 지역, 경험 태그 (Backend C)
└── admin      콘텐츠 관리, 시드 데이터 (Backend C)
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

- JWT 인증 필터 (`SecurityConfig`의 TODO). 지금은 토큰을 해석하지 않아 회원 API가 모두 401을 반환합니다.
- 카카오 로그인 연동
- 각 도메인의 엔티티와 API

## 문서

기획 문서(PRD, 기능명세서, ERD, API명세서)는 팀 노션에 있습니다.
