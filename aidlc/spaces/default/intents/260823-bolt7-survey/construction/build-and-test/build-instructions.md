# Build Instructions — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). Bolt 3 상속·적응. -->

## 사전 요구사항
- Java 21, Node 20+/npm, Docker(PostgreSQL·Testcontainers). 시크릿 `.env`(커밋 금지).

## 환경 설정
1. `cp .env.example .env`. 2. `docker-compose up -d`(PostgreSQL 16). 3. 백엔드 `SPRING_PROFILES_ACTIVE=local`. Flyway V1~V5 적용(V5=survey_feedback).

## 빌드 명령
- 백엔드(`/backend`): `./gradlew build`. 비통합만: `./gradlew test --tests "com.learnkk.survey.*" --tests "com.learnkk.meeting.*" --tests "com.learnkk.enrollment.*" --tests "com.learnkk.auth.*" --tests "com.learnkk.kernel.*" --tests "com.learnkk.contract.*"`.
- 프론트(`/frontend`): `npm install` → `npm run build`. 개발 `npm run dev`.

## 빌드 검증
- 백엔드: BUILD SUCCESSFUL, spotless/checkstyle 0, jacoco line ≥80%. 프론트: tsc 0, vite build, eslint 0.

## 트러블슈팅
- 통합 테스트 JNA `ExceptionInInitializerError`/`Could not find a valid Docker environment`: Windows/Rancher Desktop 알려진 제약. Docker 접근 환경에서 실행. 단위/슬라이스/계약은 Docker 없이 실행.
- V5 적용 확인: 기동 로그 `Migrating schema "public" to version "5 - survey feedback"`.
