# 빌드 지침 — learnKK ux-bugfixes

## 사전 준비
- DB: `docker compose up -d db` (PostgreSQL 16, localhost:5432, db/user/pass=learnkk).
- 시크릿/환경: 루트 `.env`(예시는 `.env.example`). 백엔드는 이 값을 env로 주입받는다(DB_URL/DB_USERNAME/DB_PASSWORD/SERVER_PORT/SESSION_SECRET).

## 백엔드 빌드
- 컴파일: `cd backend && ./gradlew compileJava compileTestJava`
- 실행(로컬): 루트에서 `set -a; source .env; set +a` 후 `cd backend && ./gradlew bootRun` — 부팅 시 Flyway가 V1~V9 적용.
- V9(`meeting_session.completed`)가 이번 변경으로 추가됨. 기존 DB에 낡은 V5 이력이 있으면 스키마 리셋 후 재적용:
  `docker exec learnkk-postgres psql -U learnkk -d learnkk -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO learnkk;"`

## 프론트엔드 빌드
- 의존성: `cd frontend && npm install`
- 타입체크: `./node_modules/.bin/tsc --noEmit`
- dev 서버: `npm run dev` (포트 5173 — 백엔드 CORS 허용 오리진과 일치).

## 빌드 검증
- 백엔드 `compileJava`·`compileTestJava` BUILD SUCCESSFUL.
- 프론트 `tsc --noEmit` 종료코드 0.
- 앱 부팅 로그에 "now at version v9" 확인.

## 트러블슈팅
- 8080 포트 점유 시: `netstat -ano | grep :8080` 로 PID 확인 후 종료.
- CORS 차단(브라우저): 프론트 포트를 5173으로(백엔드 `learnkk.cors.allowed-origins` 기본값과 일치).
