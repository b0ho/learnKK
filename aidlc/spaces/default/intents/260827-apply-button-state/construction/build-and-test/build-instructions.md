# Build Instructions — apply-button-state

## Backend
```
cd backend && ./gradlew clean build      # 컴파일 + 테스트
./gradlew test                           # 테스트만
```
- Java/Spring, Gradle(build.gradle). 통합테스트는 Docker(Testcontainers) 필요.

## Frontend
```
cd frontend && npm ci
npm run build     # tsc -b && vite build (프로덕션)
npm run dev       # 로컬 개발 서버
```
- Vite + React + TypeScript. 산출물 `dist/`.
