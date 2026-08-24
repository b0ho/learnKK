# 기술 스택 — learnKK

## 프론트엔드
- React 18 + TypeScript, Vite 5 (dev 서버 포트 5173 — 백엔드 CORS 기본값과 일치).
- 라우팅 react-router-dom. UI: shadcn/ui 계열 컴포넌트(Radix + Tailwind), lucide-react 아이콘.
- 테스트: Vitest + React Testing Library + @testing-library/user-event.
- 린트/포맷: ESLint(+@typescript-eslint) + Prettier.

## 백엔드
- Java 21 + Spring Boot 3.x, Gradle.
- Spring Web(MVC), Spring Data JPA(Hibernate 6.5), Flyway(마이그레이션 V1~V8).
- 인증: 자체 세션 토큰 + 인터셉터. 비밀번호 해시 bcrypt(Spring Security 기본).
- 테스트: JUnit 5, Spring Boot Test, MockMvc(@WebMvcTest), Mockito, Testcontainers(PostgreSQL), 계약 테스트(OpenApiContractTest).
- 포맷/린트: Spotless + google-java-format, Checkstyle.

## 데이터/인프라
- PostgreSQL 16 (docker-compose, 로컬 `learnkk-postgres` :5432, db/user/pass=learnkk).
- 시크릿은 `.env`(gitignore) + `.env.example`, Spring profile로 주입.

## 계약
- OpenAPI 3.0.3 (`contracts/openapi.yaml`, 현재 0.7.0-bolt7): 37 paths, 42 schemas.
