# learnKK (런크크)

사내 멘토링 러닝 플랫폼. 이 저장소는 **Bolt 7 (Survey/Feedback)** 까지 반영되어 있습니다. Bolt 3 신청(U4) 위에 사전설문 응답·과정 피드백(U8)을 얹습니다:

> 개설(PENDING_APPROVAL) → ① 승인(RECRUITING) → 모집확정(READY_TO_START | CANCELLED) → ② 시작(IN_PROGRESS) → ③ 완료(COMPLETED) · 반려/취소

- 불법 전이는 항상 409 `MEETING_INVALID_TRANSITION`(또는 완료 게이트의 `MEETING_SESSIONS_NOT_ENDED`)로 거부됩니다.
- **신청(Bolt 3)**: 멘티는 RECRUITING 모임에 선착순으로 신청합니다. 정원 초과 금지(overbooking 없음, BR-U4-1)는 모임 단위 어드바이저리 락(`pg_advisory_xact_lock`) + 활성 신청 수 판정으로 보장하고, 중복 신청은 `UNIQUE(meeting_id, mentee_id)`로 차단합니다. ②시작 전에는 취소 가능(취소 시 빈자리 복귀).
- **사전설문 응답(Bolt 7)**: 참여 멘티는 ②시작(IN_PROGRESS) 이후에만 사전설문에 응답합니다(그 외 409 `PRESURVEY_NOT_OPEN`, 필수 미응답 400). 응답 열람(getAnswers)은 인가 기준만(소유 멘토·관리자·본인, 상태 게이팅 없음). 문항당 1응답(`UNIQUE(question_id, mentee_id)`, 재제출 갱신).
- **과정 피드백(Bolt 7)**: 참여 멘티는 IN_PROGRESS/COMPLETED 모임에 피드백을 제출합니다(모임당 1건, `UNIQUE(meeting_id, mentee_id)`). 열람은 소유 멘토·관리자만(타 모임 멘토 403 `FEEDBACK_FORBIDDEN`, 멘티 열람 경로 없음).

## 구조 (Monorepo)

| 경로 | 내용 |
| --- | --- |
| `/backend` | Spring Boot 3.x · Java 21 · Gradle. 3계층(Controller→Service→Repository), 패키지 루트 `com.learnkk` (모듈: `kernel`, `auth`, `meeting`, `enrollment`, `survey`) |
| `/contracts` | `openapi.yaml` — API 계약(#1), Bolt 7 = `0.4.0-bolt7` |
| `/frontend` | React · TypeScript · Vite · shadcn/ui. 단일 API 클라이언트(`src/api/client.ts`), Vitest + RTL |
| `docker-compose.yml` | 로컬 PostgreSQL 16 |
| `.env.example` | 로컬 환경변수/시크릿 예시 (`.env`는 커밋 금지) |

## 사전 요구

- Java 21 (Corretto 등)
- Docker / Docker-compatible 런타임 (PostgreSQL, 통합 테스트용)

## 로컬 실행

1. 환경변수 준비

   ```bash
   cp .env.example .env
   # .env 를 열어 DB_PASSWORD, SESSION_SECRET 등을 로컬 값으로 채웁니다.
   ```

2. PostgreSQL 기동

   ```bash
   docker compose up -d db
   ```

3. 백엔드 실행 (Flyway가 기동 시 스키마를 마이그레이션합니다)

   ```bash
   cd backend
   # .env 값을 셸로 로드한 뒤 실행
   set -a && source ../.env && set +a
   ./gradlew bootRun
   ```

   기본 포트는 `8080` 입니다. 프로필을 나눠 쓰려면 `SPRING_PROFILES_ACTIVE=local` 로 `application-local.yml` 을 활성화할 수 있습니다.

## 빌드 & 테스트

```bash
cd backend
./gradlew build          # 컴파일 + 테스트 + 커버리지 게이트(80% line floor) + Spotless + Checkstyle
./gradlew test           # 테스트만
```

### 통합 테스트와 Docker

통합 테스트는 Testcontainers로 실제 PostgreSQL을 띄웁니다. 기본 Docker 소켓이 아닌 환경(예: Rancher Desktop)에서는 아래처럼 Docker 설정을 전달하세요. `build.gradle`의 test 태스크가 이 값들을 포크된 테스트 JVM으로 전달합니다.

```bash
DOCKER_HOST=unix://$HOME/.rd/docker.sock \
DOCKER_API_VERSION=1.43 \
TESTCONTAINERS_RYUK_DISABLED=true \
./gradlew test
```

표준 Docker Desktop 환경에서는 별도 설정 없이 `./gradlew test` 로 동작합니다.

## API 계약

- OpenAPI 스펙: [`contracts/openapi.yaml`](contracts/openapi.yaml)
- 응답 스키마 계약 테스트: `backend/src/test/java/com/learnkk/contract/OpenApiContractTest.java` — 응답 DTO 직렬화 결과가 스펙과 일치하는지 검증합니다.

### 엔드포인트 요약

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | - | 가입 (MENTOR/MENTEE) |
| POST | `/api/auth/login` | - | 로그인 (세션 토큰 발급) |
| POST | `/api/auth/logout` | Bearer | 로그아웃 (세션 폐기) |
| GET | `/api/users/me/profile` | Bearer | 내 프로필 조회 |
| PUT | `/api/users/me/profile` | Bearer | 내 프로필 수정 |
| POST | `/api/meetings` | Bearer (MENTOR) | 모임 개설 (PENDING_APPROVAL) |
| GET | `/api/meetings/{id}` | - | 모임 상세 |
| GET | `/api/meetings?status=recruiting` | - | 모집중 목록 |
| GET | `/api/meetings/mine` | Bearer (MENTOR) | 내 모임 목록 (운영 허브, Bolt 2) |
| PUT | `/api/meetings/{id}/questions` | Bearer (소유 MENTOR) | 사전설문 문항 교체 |
| GET | `/api/meetings/{id}/questions` | - | 사전설문 문항 조회 |
| POST | `/api/meetings/{id}/enrollments` | Bearer (MENTEE) | 모임 신청 (선착순, 정원/중복 제어, Bolt 3) |
| DELETE | `/api/meetings/{id}/enrollments/mine` | Bearer (MENTEE) | 내 신청 취소 (②시작 전, Bolt 3) |
| GET | `/api/meetings/{id}/applicants` | Bearer (소유 MENTOR/ADMIN) | 신청자 목록 (Bolt 3) |
| GET | `/api/enrollments/mine` | Bearer | 내 신청 현황 (Bolt 3) |
| POST | `/api/meetings/{id}/survey-answers` | Bearer (참여 MENTEE) | 사전설문 응답 제출 (②후, Bolt 7) |
| GET | `/api/meetings/{id}/survey-answers/mine` | Bearer | 내 사전설문 응답 조회 (Bolt 7) |
| GET | `/api/meetings/{id}/mentees/{menteeId}/survey-answers` | Bearer (소유 MENTOR/ADMIN/본인) | 멘티 사전설문 응답 열람 (Bolt 7) |
| POST | `/api/meetings/{id}/feedback` | Bearer (참여 MENTEE) | 과정 피드백 제출 (Bolt 7) |
| GET | `/api/meetings/{id}/feedback` | Bearer (소유 MENTOR/ADMIN) | 과정 피드백 목록 열람 (Bolt 7) |
| POST | `/api/admin/meetings/{id}/approve` | Bearer (ADMIN) | ① 개설 승인 (T1) |
| POST | `/api/admin/meetings/{id}/reject` | Bearer (ADMIN) | 반려 (T2, 사유 필수) |
| POST | `/api/admin/meetings/{id}/confirm-recruitment` | Bearer (ADMIN) | 모집확정 진행/취소 (T3/T4) |
| POST | `/api/admin/meetings/{id}/approve-start` | Bearer (ADMIN) | ② 시작 승인 (T5) |
| POST | `/api/admin/meetings/{id}/complete` | Bearer (ADMIN) | ③ 완료 (T6, 세션 종료 게이트) |

에러 응답 본문은 `ErrorPayload{code, message, details}` 형태이며, 상태코드는 400(검증)/401(인증)/403(인가)/404(미존재)/409(상태충돌·중복)로 매핑됩니다.

## Bolt 7 범위 / 제외

- **포함 (Bolt 7)**: U8 Survey/Feedback — 사전설문 응답 제출(②후 게이팅·필수 검증·문항당 upsert, W1/BR-U8-1), 응답 열람(소유 멘토·관리자·본인, 상태 게이팅 없음, W2/BR-U8-2), 과정 피드백 제출(참여 멘티·IN_PROGRESS/COMPLETED, 모임당 upsert, W3/BR-U8-3), 피드백 열람(소유 멘토·관리자만, W4/BR-U8-4). 문항 틀은 U3 read(`SurveyTemplateService.getQuestions`), 참여자 판정은 U4 read(`EnrollmentService.isActiveParticipant`) — U8은 U3/U4 테이블을 직접 건드리지 않습니다. FE: 멘티 사전설문 응답·피드백 화면, 멘토/관리자 피드백·응답 열람 화면(`features/survey`), 내 러닝/운영 허브 배선.
- **포함 (Bolt 3)**: U4 Enrollment — 멘티 선착순 신청(정원/중복 제어, BR-U4-1/2), 신청 취소(②시작 전, BR-U4-3), 신청자 목록(소유 멘토/관리자, US-2.3), 멘티 신청 현황(`listMyEnrollments`, US-3.5). FE: 모집중 목록의 인라인 신청 버튼(MENTEE), 내 러닝의 신청 현황·취소, 멘토 운영 허브의 신청자 목록/수 배선.
- **동시성 불변식(BR-U4-1)**: 신청 트랜잭션에서 `pg_advisory_xact_lock(meetingId)` → 활성(APPLIED) 신청 수 count → 정원 미만이면 insert. 락은 트랜잭션 종료까지 유지되어 잔여 1석 경합에서도 1건만 성공합니다. U3 `meetings` 행은 잠그지 않고 `MeetingService.getMeeting` read로만 접근합니다(모듈 소유 규칙).
- **이월 (Bolt 8+)**: 관리자 승인 큐 목록 조회(U9/Bolt 8), 자료/쪽지/모니터링, CI/CD·배포. 과정 설문은 자유 서술(content) 구조이며 고정 문항 셋은 이월. 대기열(waitlist)·취소 후 재신청은 설계상 없음(unique 유지).
- **Forward seam**: 완료(T6)의 "모든 세션 종료" 판정은 `meeting/service/SessionCompletionGate` 인터페이스 뒤로 분리되어 있으며, 현재 스텁(`NoSessionsCompletionGate`)은 통과(true)를 반환합니다. Bolt 6(U5)가 실제 세션 read 구현으로 교체합니다.

## 프론트엔드 (`/frontend`)

```bash
cd frontend
npm install
npm run dev            # Vite 개발 서버 (기본 VITE_API_BASE=http://localhost:8080)
npm run build          # tsc + vite build (타입 에러 0)
npm run test -- --run  # Vitest + RTL
npm run lint           # ESLint
```

## 주요 규약

- Entity는 API 경계에 노출하지 않습니다 (Controller는 Request/Response DTO만 사용).
- JPA 물리 네이밍 = snake_case, Jackson = camelCase (명시 설정).
- 비밀번호는 bcrypt(`PasswordEncoder`)로 저장, 세션은 자체 토큰(`sessions` 테이블).
- DB enum은 varchar + CHECK 제약, 스키마는 Flyway 마이그레이션(`backend/src/main/resources/db/migration`)으로 관리.
- 시크릿은 커밋하지 않습니다 (`.env` gitignore, 환경변수/Spring profile 주입).
