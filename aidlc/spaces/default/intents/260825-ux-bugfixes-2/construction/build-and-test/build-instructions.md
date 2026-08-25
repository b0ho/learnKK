# Build Instructions — ux-bugfixes-2

## Backend
```bash
cd backend
./gradlew compileJava compileTestJava   # 컴파일
./gradlew build                          # 컴파일 + 테스트 + 커버리지 + Spotless/Checkstyle
```
- Java 21, Gradle. Flyway가 기동 시 V1~V12 마이그레이션 적용(로컬 Postgres 필요: `docker compose up -d db`).

## Frontend
```bash
cd frontend
npm install
npm run build          # tsc -b + vite build (타입 에러 0)
```

## 로컬 실행
```bash
docker compose up -d db
cd backend && set -a && source ../.env && set +a && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # :8080
cd frontend && npm run dev   # :5173 (CORS 허용 오리진)
```
