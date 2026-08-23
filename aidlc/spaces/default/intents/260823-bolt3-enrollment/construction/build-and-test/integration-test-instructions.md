# Integration Test Instructions — Bolt 3 Enrollment (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard(핵심 경계 통합). 정원 무결성이 핵심. -->

## 프레임워크
- Testcontainers PostgreSQL 16 + Spring Boot Test(full context) + Flyway(V1~V4) + `ddl-auto=validate`. 계약: OpenApiContractTest.

## 실행
- `cd backend && ./gradlew test`(통합 포함). 통합만 `--tests "com.learnkk.integration.*"`. Docker 데몬 필요.

## 핵심 경계 시나리오 (Bolt 3)
- **정원 무결성(EnrollmentConcurrencyIntegrationTest)**: capacity=1에 N-thread 동시 apply → 정확히 1 APPLIED + 나머지 ENROLLMENT_FULL. 동시 중복 apply → 1건(unique).
- **관통(EnrollmentIntegrationTest)**: 신청→listApplicants→취소→빈자리 재신청 end-to-end. ②후 취소 409, 비RECRUITING 신청 409.
- 인가: 비멘티 apply 403, 소유자외 listApplicants 403.

## 목표
- 무결성(overbooking 금지) 필수 커버 + 주요 실패 분기(409/403).

## 알려진 제한 / 라이브 검증 대체
- 이 환경(Windows/Rancher Desktop) docker-java JNA 초기화 실패로 Testcontainers 미실행 — 코드 결함 아님(Bolt 1/2 동일).
- **대체 실증(라이브 API E2E, 실제 Postgres)**: capacity=1 + 3 병렬 apply → 정확히 1×201 + 2×409, DB APPLIED count=1. 중복 409, 취소→재신청, ②후 취소 409, 인가 403 — 전부 통과(build-test-results.md).
