# Build Instructions — Bolt 6 Session/Attendance (learnKK)

<!-- build-and-test 산출물(quality 리드). 입력: construction/bolt6-session/code-generation/code-generation-plan.md·code-summary.md. -->

## 사전 요구
- Java 21 (Corretto 21 확인), Gradle wrapper(`./gradlew`, 8.10.2).
- Node/npm (Vite 5 / Vitest 2.1), 프론트 `frontend/`.
- PostgreSQL 16 — docker-compose `db` 서비스. clone별 `.env`(이 clone: 포트 5435, 컨테이너 `learnkk-postgres-3`, DB/USER/PW=learnkk).

## 환경 설정
- 백엔드 datasource는 환경변수 주입(비밀값 비커밋, project.md): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`(이 clone 8083), `SESSION_SECRET`.
- `spring.jpa.hibernate.ddl-auto=validate` — 스키마는 Flyway가 소유. 신규 `V5__session.sql`이 기동 시 적용된다(V4 enrollment 위에).

## 빌드 명령
- 백엔드 컴파일: `./gradlew compileJava compileTestJava`.
- 백엔드 부트 jar: `./gradlew bootJar -x test` → `build/libs/learnkk-backend-0.0.1-SNAPSHOT.jar`.
- 프론트: `npm ci` 후 `npx tsc -b && npx vite build`.

## 빌드 검증
- 컴파일 BUILD SUCCESSFUL, Spotless(googleJavaFormat)·Checkstyle(advisory) clean.
- 앱 기동 시 Flyway "validated 5 migrations, schema version 5", Hibernate 엔티티 validate 통과, Tomcat 기동 로그 확인.

## 트러블슈팅
- 기동 시 빈 이름/엔티티명 충돌(예: `sessionRepository` 중복) → 신규 타입 simple name이 auth 등 기존 도메인과 겹치지 않게 접두(`MeetingSession`). (이번 Bolt에서 실제 발생·수정, code-summary 참조.)
- Testcontainers 통합 테스트 실패(`DockerClientProviderStrategy`) → Windows/Rancher JNA 이슈. 코드 결함 아님. 라이브 E2E로 대체.
