# Integration Test Instructions — Bolt 2 Meeting 완성 (learnKK)

<!-- build-and-test 산출물(quality 리드). Test Strategy=Standard(핵심 경계 통합). 출처: code-generation-plan.md·code-summary.md, team.md(Testcontainers). -->

## 프레임워크·설정

- **Testcontainers PostgreSQL 16** + Spring Boot Test(full context) + Flyway(V1~V3) + `ddl-auto=validate`.
- **계약 테스트**: `OpenApiContractTest`(swagger-parser로 `contracts/openapi.yaml` 파싱, 응답 DTO 직렬화 정합).

## 실행 방법

- 전체: `cd backend && ./gradlew test`(통합 포함). 통합만: `--tests "com.learnkk.integration.*"`.
- Docker 데몬 접근 필요(build-instructions.md 트러블슈팅 참조).

## 핵심 경계 시나리오 (Bolt 2)

- **상태머신 관통**: 개설(PENDING_APPROVAL) →①승인(RECRUITING) →모집확정 proceed=true(READY_TO_START) →②시작(IN_PROGRESS) →③완료(COMPLETED) end-to-end 통과.
- **불법 전이**: 모집확정 전 ②시작 시도 → 409. 이중 완료 → 409. 종료 상태 재액션 → 409.
- **취소 경로**: 모집확정 proceed=false → CANCELLED, reject_reason 저장 확인. 사유 누락 → 400.
- **인가 경계**: 비관리자 전이 액션 403, 비멘토 listMyMeetings 403, 미인증 401.
- **계약 정합**: 신규 admin 라우트·`/api/meetings/mine`·`ConfirmRecruitmentRequest` 응답이 openapi 스키마와 일치.

## 커버리지 목표

- 관통 흐름 + 주요 실패 분기(409/403/400) 각 1건 이상. `MeetingIntegrationTest`·`OpenApiContractTest`가 커버.

## 알려진 제한

- 이 환경(Windows/Rancher Desktop)에서 docker-java JNA 초기화 실패로 통합 테스트 미실행 — 코드 결함 아님(Bolt 1 AuthIntegrationTest 동일). Docker 접근 가능 환경에서 통과 예상.
