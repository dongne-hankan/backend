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

## 폴더 구조

```text
src/main/java/com/sixteez/dongnehankan
├── global     공통 설정(config), 예외(exception), 응답 형식(response)
├── auth       카카오·아이디 로그인, 회원가입, 토큰
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

## 자체 로그인

카카오 로그인과 별도로 아이디·비밀번호로 가입하고 로그인할 수 있습니다. 이메일은 받지 않으므로 비밀번호 재설정은 없고, 로그인한 상태에서 비밀번호만 바꿀 수 있습니다.

| 기능 | API |
| --- | --- |
| 아이디 중복 확인 | `GET /api/auth/login-id-availability?loginId=` |
| 자체 회원가입 | `POST /api/auth/local/signup` `{ loginId, password, nickname, privacyAgreed }` |
| 로그인 | `POST /api/auth/login` `{ loginId, password }` |
| 비밀번호 변경 | `PATCH /api/users/me/password` `{ currentPassword, newPassword }` |

아이디는 영문·숫자 4~20자이고 소문자로 바꿔 저장합니다. 비밀번호는 8~64자이며 BCrypt 해시로 저장합니다. 로그인 실패 때는 아이디와 비밀번호 중 어느 쪽이 틀렸는지 구분하지 않습니다. 비밀번호를 바꾸면 그 회원의 refresh token을 모두 폐기합니다. `GET /api/users/me`의 `loginId`는 카카오 회원이면 `null`입니다.

## 남은 일

- 온보딩 취향 설정, 조건 기억, 관리자 API (시드 데이터 입력 포함)
- 회원 탈퇴 때 카카오 연결 끊기 (Admin 키 필요)

## 문서

기획 문서(PRD, 기능명세서, ERD, API명세서)는 팀 노션에 있습니다.


## Gemini AI 연동

`feature/find-nearby`의 주변 검색·AI API를 기반으로 한다. Gemini 키는 **백엔드** `.env`의 `GEMINI_API_KEY`에만 넣고 서버를 재시작한다. 프론트의 `VITE_*` 변수에는 넣지 않는다. 키가 없으면 기본 추천으로 계속 동작한다. 모델 기본값은 `gemini-3.1-flash-lite`이며 `.env.example`에 호출 제한을 포함한 설정 예시가 있다.

| 기능 | API | AI가 하는 일 |
| --- | --- | --- |
| 지역·경험·시간 추천 | `GET /api/find/ai-recommendations?regionId=1&tagId=1&duration=MIN_30` | 규칙으로 선정한 공개 코스의 추천 이유 작성 |
| 지도 주변 산책 | `POST /api/find/nearby` | 카카오 검색 후보 안에서 장소 순서·코스 소개·선택 이유 구성 |

지역 추천의 응답은 `{ "data": { "recommendation": { ... }, "reasons": [{ "courseId": 1, "reason": "..." }], "aiGenerated": true } }`다. 기존 추천 필터, 기록 기반 순서, 제외 ID, 다른 지역을 추천하지 않는 대안 규칙은 그대로 사용한다. 이유는 `courseId`로 연결하고, 반환된 코스의 이유가 있을 때만 AI 표시를 붙인다. 주민 한마디는 원문 그대로 별도로 표시한다. AI 설명에 원래 요청한 경험·시간을 전달하여 조건 완화 코스를 원래 조건에 맞는 것으로 설명하지 않게 한다.

지역 추천은 AI를 최대 `AI_REASON_TIMEOUT_MS`(기본 1500ms)만 기다린다. 늦은 응답은 다음 요청에서 사용할 수 있으며 자동 재호출은 하지 않는다. 이유가 없으면 코스를 먼저 보여주고 사용자가 다시 확인할 수 있다. 메모리 캐시는 최대 256개·30분이며 코스 내용/주민 인용이 바뀌면 이전 이유를 재사용하지 않는다. 캐시는 DB에 저장하지 않고 서버 재시작 시 사라진다.

주변 추천 요청 예시:

```json
{ "lat": 35.858, "lng": 128.630, "duration": "MIN_30", "mood": "잠깐 걷고 싶어요" }
```

카카오 REST API 키(`KAKAO_REST_API_KEY`, 없으면 `KAKAO_CLIENT_ID`)도 필요하다. AI는 후보의 ID만 고를 수 있으며 장소 이름·주소·좌표·링크는 카카오 응답에서 가져온다. 모르는 ID·중복·빈 설명은 제외하고 머무는 시간과 장소 사이 이동 시간을 서버에서 계산해 시간 한도를 지킨다. 출발점부터 첫 장소까지의 이동은 포함하지 않으며, 이동 시간은 추정치로 실제 길찾기가 아니다. AI에 정확한 사용자 좌표, 회원 식별자, 비공개 느낌 메모를 보내지 않는다. 후보 장소의 주소·출발점과의 거리 및 사용자가 입력한 기분은 Gemini로 전송한다.

공통 클라이언트는 JSON 모드·출력 토큰 한도·동시 호출 한도를 사용한다. 잘린 응답이나 안전 필터로 중단된 응답은 사용하지 않는다. 401/403/429는 자동 재시도 없이 두 기능에서 60초간 호출을 쉬고 기본 추천을 사용한다. 로그에 키·프롬프트·응답 본문을 기록하지 않는다. 프롬프트의 데이터와 지시를 구분하고 알려지지 않은 사실·주민 인터뷰 생성을 금지하지만, 생성 문장이 사실임을 보장하지는 않으므로 사용자에게 AI 설명임을 표시한다.

검증은 `./gradlew test`로 수행한다. 자동 테스트는 HTTP 모의 응답을 사용하므로 실제 키나 API 사용량이 필요 없다. 실제 연결 확인은 로컬 서버에서 위 두 API를 호출해 지역 응답의 `reasons`와 주변 응답의 `aiGenerated`를 확인한다. local/test의 시드 주민 이야기와 좌표는 여전히 가상 예시다. Gemini를 연결해도 검증된 실제 주민 콘텐츠로 바뀌지 않는다.

Gemini REST 응답/설정은 [공식 generateContent 문서](https://ai.google.dev/api/generate-content)를 참고한다.
