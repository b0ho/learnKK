# learnKK (런크크)

사내 멘토링 러닝 플랫폼. 이 저장소는 **Bolt 1 Walking Skeleton**으로, 다음 흐름을 end-to-end로 관통합니다:

> 사번 가입 → 로그인 → (멘토) 모임 개설 → (관리자) ① 개설 승인 → 모집중(RECRUITING) 목록 노출

## 구조 (Monorepo)

| 경로 | 내용 |
| --- | --- |
| `/backend` | Spring Boot 3.x · Java 21 · Gradle. 3계층(Controller→Service→Repository), 패키지 루트 `com.learnkk` (모듈: `kernel`, `auth`, `meeting`) |
| `/contracts` | `openapi.yaml` — Bolt 1 API 계약(#1) |
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

### Bolt 1 엔드포인트 요약

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
| PUT | `/api/meetings/{id}/questions` | Bearer (소유 MENTOR) | 사전설문 문항 교체 |
| GET | `/api/meetings/{id}/questions` | - | 사전설문 문항 조회 |
| POST | `/api/admin/meetings/{id}/approve` | Bearer (ADMIN) | ① 개설 승인 (T1) |
| POST | `/api/admin/meetings/{id}/reject` | Bearer (ADMIN) | 반려 (T2) |

에러 응답 본문은 `ErrorPayload{code, message, details}` 형태이며, 상태코드는 400(검증)/401(인증)/403(인가)/404(미존재)/409(상태충돌·중복)로 매핑됩니다.

## Bolt 1 범위 / 제외

- **포함**: U1 Shared Kernel(도메인 enum·에러 규약·인증 경계·페이지네이션), U2 Auth(가입/로그인/세션/프로필), U3 Meeting 최소 슬라이스(개설·조회·모집중 목록·사전설문·상태전이 T1 승인/T2 반려).
- **제외 (Bolt 2+ 이월)**: 모집 확정, ② 시작 승인(T3~T6), ③ 완료, 신청/세션·출석/자료/쪽지/설문 응답/모니터링, 프론트엔드, CI/CD·배포. `MeetingApprovalService`에 `// Bolt 2+: T3-T6` 자리표시 주석이 있습니다.

## 주요 규약

- Entity는 API 경계에 노출하지 않습니다 (Controller는 Request/Response DTO만 사용).
- JPA 물리 네이밍 = snake_case, Jackson = camelCase (명시 설정).
- 비밀번호는 bcrypt(`PasswordEncoder`)로 저장, 세션은 자체 토큰(`sessions` 테이블).
- DB enum은 varchar + CHECK 제약, 스키마는 Flyway 마이그레이션(`backend/src/main/resources/db/migration`)으로 관리.
- 시크릿은 커밋하지 않습니다 (`.env` gitignore, 환경변수/Spring profile 주입).
