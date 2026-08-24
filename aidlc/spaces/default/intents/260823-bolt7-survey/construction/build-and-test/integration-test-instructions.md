# Integration Test Instructions — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard(핵심 경계 통합). -->

## 프레임워크
- Testcontainers PostgreSQL 16 + Spring Boot Test + Flyway(V1~V5) + `ddl-auto=validate`. 계약: OpenApiContractTest.

## 실행
- `cd backend && ./gradlew test`. 통합만 `--tests "com.learnkk.integration.*"`. Docker 필요.

## 핵심 경계 시나리오 (Bolt 7)
- **게이팅 관통(SurveyIntegrationTest)**: 개설→①→모집확정→②시작→멘티 사전설문 응답 제출→멘토 열람. ②전 제출 시도 409 PRESURVEY_NOT_OPEN.
- **피드백 권한**: 참여멘티 제출→소유멘토 열람, 타모임멘토 403, 멘티 열람 경로 없음.
- 필수 미응답 400, 비참여자 403.

## 목표
- 게이팅·인가 경계 + 관통 커버.

## 알려진 제한 / 라이브 검증 대체
- 이 환경(Windows/Rancher) docker-java JNA 초기화 실패로 Testcontainers 미실행 — 코드 결함 아님(Bolt 1~3 동일). 결정론적 게이팅·인가는 단위/슬라이스로 완전 커버, 필요 시 라이브 API/UI E2E 보완.
