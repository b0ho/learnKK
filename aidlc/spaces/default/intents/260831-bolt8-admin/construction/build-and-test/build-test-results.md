# Build & Test Results — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(실제 실행 결과). 2026-08-31. 로컬(macOS) 실측. -->

## 실행 환경
- Java 21 + Gradle 8.10.2, Node/npm(Vitest/Vite). Docker Desktop(Testcontainers 가용 — Postgres 16). Flyway V1~V10(변경 없음).

## 백엔드 (`/backend`)
- **1차 `./gradlew build`**: compileTestJava 실패 11건 — FR-7 `mentorCompletionStatus` 추가 이후 미갱신된 구버전 record 생성자(파킹 트리 잔재). → 8개 테스트 파일 현행화 후 컴파일 통과.
- **2차 `./gradlew test`**: **321 테스트 중 1 실패** — `EnrollmentIntegrationTest:84`, FR-12(취소 행 재사용 재신청) 도입 이후 미갱신 기대값(재신청 시 실제 ENROLLMENT_FULL, 기대 DUPLICATE). → 현행 규칙으로 수정 + APPLIED 중복 검증 추가.
- **최종 `./gradlew test`**: **전체 통과(그린)** — 신규 `AdminMonitoringServiceTest`(4)·`AdminMonitoringControllerTest`(5) 포함. 통합 테스트는 Testcontainers로 실 Postgres에서 실행됨.

## 프론트엔드 (`/frontend`)
- **1차 `npm run build`**: `AppShell.tsx:42` tsc 에러(`as const` PATHS 리터럴 유니온 vs `includes(string)`). → `TAB_ROOTS: readonly string[]` 명시 후 **빌드 그린**.
- **테스트**: `AdminMonitoringPage.test.tsx` 초기 1건 실패 — '진행중' 텍스트가 필터 버튼·카드 뱃지 중복 매칭(TestingLibraryElementError). → `within(card)` 스코프로 수정 후 **전체 통과**. `admin.test.ts` listMonitoring 2건 포함.

## 인가·경계 검증 (단위/슬라이스 커버)
- 모니터링 조회: 관리자 200 · MENTOR/MENTEE 403 `MONITORING_FORBIDDEN` · 미인증 401 · 잘못된 status 400 `VALIDATION_FAILED`.
- 집계식: 총출석 3/(세션 2×멘티 2)=0.75, 세션·멘티 0 → 0.0(0나눗셈 방어), 수료 확정/후보 카운트 분리, 종료 세션 판정(수동 완료·시간창 경과).
- FE: 필터 선택 시 status 쿼리 재조회(전체=파라미터 생략), 빈 목록·API 에러 노출.

## 수동 시나리오 (라이브 확인)
- 관리자(V10 시드) 로그인 → 관리 탭 → '운영 현황' → 모임 카드(상태·출석율·수료 진행) 표시, 필터 전환, 승인 큐 복귀 — MANUAL-TEST-SCENARIO 보완 항목으로 권고.
