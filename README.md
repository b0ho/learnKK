# learnKK (런크크)

사내 멘토링 러닝 플랫폼. 이 저장소는 **Bolt 2 (Meeting 완성)** 까지 반영되어 있습니다. Bolt 1 Walking Skeleton 위에 모임 상태머신을 완결합니다:

> 개설(PENDING_APPROVAL) → ① 승인(RECRUITING) → 모집확정(READY_TO_START | CANCELLED) → ② 시작(IN_PROGRESS) → ③ 완료(COMPLETED) · 반려/취소

불법 전이는 항상 409 `MEETING_INVALID_TRANSITION`(또는 완료 게이트의 `MEETING_SESSIONS_NOT_ENDED`)로 거부됩니다.

## 구조 (Monorepo)

| 경로 | 내용 |
| --- | --- |
| `/backend` | Spring Boot 3.x · Java 21 · Gradle. 3계층(Controller→Service→Repository), 패키지 루트 `com.learnkk` (모듈: `kernel`, `auth`, `meeting`) |
| `/contracts` | `openapi.yaml` — API 계약(#1), Bolt 2 = `0.2.0-bolt2` |
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
| POST | `/api/admin/meetings/{id}/approve` | Bearer (ADMIN) | ① 개설 승인 (T1) |
| POST | `/api/admin/meetings/{id}/reject` | Bearer (ADMIN) | 반려 (T2, 사유 필수) |
| POST | `/api/admin/meetings/{id}/confirm-recruitment` | Bearer (ADMIN) | 모집확정 진행/취소 (T3/T4) |
| POST | `/api/admin/meetings/{id}/approve-start` | Bearer (ADMIN) | ② 시작 승인 (T5) |
| POST | `/api/admin/meetings/{id}/complete` | Bearer (ADMIN) | ③ 완료 (T6, 세션 종료 게이트) |

에러 응답 본문은 `ErrorPayload{code, message, details}` 형태이며, 상태코드는 400(검증)/401(인증)/403(인가)/404(미존재)/409(상태충돌·중복)로 매핑됩니다.

## Bolt 2 범위 / 제외

- **포함 (Bolt 2)**: U3 Meeting 상태머신 완결 — 모집확정(T3)/모집취소(T4)/시작 승인(T5)/완료(T6), 반려·취소 사유 필수화, 문항 빌더 게이팅(②시작 이후 잠금, BR-U3-7), 멘토 운영 허브(`listMyMeetings` + FE 목록·상태·다음 액션), 관리자 상태 인지 액션 FE.
- **이월 (Bolt 3+)**: 신청(U4/Bolt 3), 세션·출석(U5/Bolt 6), 설문 응답(U8/Bolt 7), 관리자 승인 큐 목록 조회(U9/Bolt 8), 자료/쪽지/모니터링, CI/CD·배포.
- **Forward seam**: 완료(T6)의 "모든 세션 종료" 판정은 `meeting/service/SessionCompletionGate` 인터페이스 뒤로 분리되어 있으며, Bolt 2 스텁(`NoSessionsCompletionGate`)은 통과(true)를 반환합니다. Bolt 6(U5)가 실제 세션 read 구현으로 교체합니다.

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
