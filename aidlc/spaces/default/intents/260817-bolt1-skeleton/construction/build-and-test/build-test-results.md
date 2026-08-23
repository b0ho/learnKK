# Build & Test Results — Bolt 1 Walking Skeleton (learnKK)

<!-- build-and-test 산출물(Step 10 실제 실행 결과). 출처: code-generation-plan.md·code-summary.md 대상 코드에 대한 실행. 이 세션에서 실측(2026-08-18 KST). -->

## 실행 환경

- Java 21.0.7 LTS, Gradle 8.10.2(wrapper), Node/npm(Vite), Docker(Rancher Desktop, `unix://~/.rd/docker.sock`, API 1.43, Ryuk disabled).

## 백엔드 (`/backend`)

- **빌드**: `./gradlew clean test jacocoTestReport` → **BUILD SUCCESSFUL**.
- **테스트**: **69개 / 11개 클래스, 실패 0, 에러 0, 스킵 0.** (단위 + @WebMvcTest + OpenApiContractTest + Testcontainers 통합 2종 포함)
- **커버리지(JaCoCo LINE)**: covered 424 / missed 48 → **89.8%** (80% floor 통과, `jacocoTestCoverageVerification` OK).
- **정적 검사**: Spotless(google-java-format)·Checkstyle 위반 0.

## 프론트엔드 (`/frontend`)

- **빌드**: `npm run build`(`tsc -b && vite build`) → 1674 모듈 변환, 타입 에러 0, dist 산출.
- **테스트**: `npm run test -- --run` → **59개 / 14개 파일, 전부 통과.**
- **커버리지(v8)**: All files — Stmts **95.22%**, Branch 84.74%, Funcs 84.31%, Lines **95.22%** (80% floor 통과).
- **Lint**: ESLint 에러 0.

## 관통 시나리오 실증

MeetingIntegrationTest(Testcontainers PostgreSQL 16)가 가입 → 로그인 → 모임 개설(PENDING_APPROVAL) → 관리자 ① 승인(RECRUITING) → 모집중 목록 노출을 end-to-end로 통과. 이중 승인 409·비관리자 403 경계 포함.

## 실패·조치

- 실패 없음. 진단/수정 반복 불필요.

## 알려진 제한

- 통합 테스트는 Docker 필요(미가용 환경에서는 단위 테스트만 실행). Rancher Desktop은 build-instructions.md의 환경변수 필요.
- Bolt 1 최소 슬라이스 범위 — U3 ②/③/모집확정, U4~U9, CI/배포는 미구현(이월).
