# Build & Test Results — apply-button-state

실행 일시: 2026-08-27 · 브랜치 `bugfix/apply-button-state`

## Backend (Gradle)
- `./gradlew compileJava compileTestJava` — **PASS** (green).
- `./gradlew test` — **314 tests, 21 failed**.
  - 통과 293: 단위/서비스/웹/계약 테스트 전부(신규 회귀 포함).
    - `MeetingControllerTest`: 모집 목록 `enrolledCount`/`full` 보강 검증 PASS.
    - `EnrollmentServiceTest`: `activeCountsByMeeting` 매핑/빈입력 PASS.
    - `OpenApiContractTest`: 갱신된 MeetingSummary 스키마 정합 PASS.
  - 실패 21: **모두 `*IntegrationTest`** — Testcontainers Docker 클라이언트 초기화 실패(`NoClassDefFoundError`/`ExceptionInInitializerError at DockerClientProviderStrategy`). 이 JVM/Testcontainers 환경 비호환 문제로, **이번 버그와 무관한 환경 제약**(이전 인텐트에서도 동일하게 관측). 코드 로직 실패 아님.

## Frontend (npm)
- `tsc --noEmit` (루트 tsconfig) — **PASS**.
- `vitest run` — **28 files, 137 tests PASS** (신규: 로드 시 신청완료 반영 / 마감 배지·비활성).
- `eslint .` — **0 errors** (1 warning: SurveyBuilder fast-refresh, 사전 존재).
- `npm run build` (`tsc -b && vite build`) — **PASS** (1696 modules, ✓ built). 게이트 승인(Q1=A)에 따라 사전 존재 타입에러 `src/routes/AppShell.tsx`(`TAB_ROOTS`를 `readonly string[]`로 타입 지정, `.includes(string)` 허용)를 함께 수리해 프로덕션 빌드 green 복구. 이번 버그와 무관한 사전 이슈였음.

## 판정
- 이번 버그 관련 변경(FR-1~FR-4)은 백엔드 단위/웹/계약 + 프론트 단위/타입체크/프로덕션 빌드로 **검증 완료(green)**.
- 잔여 red 1건은 통합테스트 Docker 초기화 실패(환경 제약, 코드 무관) — 정상 Docker 환경 재실행 권장.
