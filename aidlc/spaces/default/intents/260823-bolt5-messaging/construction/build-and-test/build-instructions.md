# Build Instructions — Bolt 5 Messaging (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드). 상류: bolt5-messaging code-generation-plan.md · code-summary.md. -->

## 전제 (Prerequisites)

- **JDK 21**, Gradle wrapper(8.10.2 동봉). **Node.js + npm**(Vite 5 / Vitest 2).
- **PostgreSQL 16**(로컬 `docker-compose up db`). 마이그레이션 Flyway V1~**V5**(V5 = messaging).
- 시크릿은 `.env`(비커밋). `DB_URL/DB_USERNAME/DB_PASSWORD`, `SESSION_SECRET`.

## 의존성 설치·환경 설정

- Backend: `backend/`에서 `./gradlew build`가 의존성 해석. DB 기동: 루트 `docker-compose up -d db`.
- Frontend: `frontend/`에서 `npm install`. `VITE_API_BASE`(기본 `http://localhost:8080`).

## 빌드 명령 (Build)

- **Backend**: `cd backend && ./gradlew clean build` — compile + spotless + checkstyle + test + jacoco 검증(LINE ≥80%).
  - 컴파일만: `./gradlew compileJava compileTestJava`.
- **Frontend**: `cd frontend && npm run build` (= `tsc -b && vite build`).

## 빌드 검증 (Verification)

- Backend: `BUILD SUCCESSFUL`, `spotlessCheck`·`checkstyleMain/Test` 통과.
- Frontend: 타입 에러 0, `dist/` 산출(index js ~299KB).
- 실측 결과는 `build-test-results.md` 참조.

## 트러블슈팅

- **Flyway validate 실패**: DB 스키마와 마이그레이션 불일치 → 로컬 DB를 초기화(`docker-compose down -v && up -d db`) 후 재기동.
- **CRLF/LF spotless 위반**: `./gradlew spotlessApply`로 정규화.
- **포트 충돌**: BE 8080(`SERVER_PORT`), FE dev 5175(vite.config).
