# Build Instructions — Bolt 1 Walking Skeleton (learnKK)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). 출처: code-generation-plan.md·code-summary.md(생성 코드/스택), team.md(docker-compose·80% floor), project.md(시크릿 비커밋). -->

## 사전 요구사항

- **Java 21**(LTS) — 백엔드 빌드/실행.
- **Node 20+ / npm** — 프론트엔드 빌드/테스트.
- **Docker**(로컬 PostgreSQL 및 Testcontainers 통합 테스트). 이 개발 환경은 Rancher Desktop 사용 — 통합 테스트 시 아래 환경변수 필요.
- 시크릿은 `.env`로 주입(커밋 금지). `.env.example` 복사 후 값 채움(project.md Forbidden).

## 환경 설정

1. `cp .env.example .env` 후 DB 자격증명·세션 시크릿 값 주입.
2. 로컬 PostgreSQL 기동: 루트에서 `docker-compose up -d`(PostgreSQL 16).
3. (Rancher Desktop 통합 테스트 전용) 환경변수:
   ```
   export DOCKER_HOST=unix://$HOME/.rd/docker.sock
   export DOCKER_API_VERSION=1.43
   export TESTCONTAINERS_RYUK_DISABLED=true
   ```
   표준 Docker Desktop에서는 불필요.

## 빌드 명령

- **백엔드**(`/backend`): `./gradlew build` (compile + test + spotless + checkstyle + jacoco). 실행: `./gradlew bootRun`. 배포 산출물: `./gradlew bootJar`.
- **프론트엔드**(`/frontend`): `npm install` → `npm run build`(`tsc -b && vite build`). 개발 서버: `npm run dev`(`VITE_API_BASE` 기본 `http://localhost:8080`).

## 빌드 검증

- 백엔드: `BUILD SUCCESSFUL`, checkstyle/spotless 위반 0, jacoco line coverage ≥80% floor(`jacocoTestCoverageVerification`).
- 프론트엔드: `tsc` 타입 에러 0, `vite build` 청크 생성, `eslint` 에러 0.

## 트러블슈팅

- 통합 테스트 `Could not find a valid Docker environment`: 위 Rancher 환경변수 확인(또는 Docker Desktop 기동).
- `ddl-auto=validate` 스키마 불일치: Flyway 마이그레이션(V1~V3)과 엔티티 매핑 확인.
- FE `VITE_API_BASE` 미설정 시 기본 localhost:8080 — 백엔드 미기동 시 네트워크 에러(로그인/목록 호출).
