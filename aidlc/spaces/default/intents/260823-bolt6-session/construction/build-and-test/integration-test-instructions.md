# Integration Test Instructions — Bolt 6 Session/Attendance (learnKK)

<!-- build-and-test 산출물. 입력: construction/bolt6-session/code-generation/code-generation-plan.md·code-summary.md. 관통·경계 검증. -->

## Testcontainers 통합 테스트
- `SessionAttendanceIntegrationTest`(AbstractIntegrationTest 상속, Testcontainers Postgres 16). 관통: 세션 생성→시간창 내 checkIn→출석율→computeCompletion→④ approve. 창밖 409·멱등 포함.
- 실행: `./gradlew test --tests "com.learnkk.integration.*"` (Docker 소켓 필요).
- 이 환경 제약: Windows/Rancher Desktop JNA로 `DockerClientProviderStrategy` 실패 → 미실행. 코드 결함 아님. 아래 라이브 E2E로 대체 실증.

## 라이브 E2E 대체 (실행 앱 + 실제 Postgres)
- 절차: docker-compose `db`(포트 5435) 기동 상태에서 부트 jar를 `.env` 환경변수로 실행(포트 8083, Flyway V5 적용) → curl로 REST 관통 검증.
- ADMIN 확보: MENTEE 가입 후 `update users set role='ADMIN'` 승격 후 로그인(세션이 역할 캡처). 로그인은 nickname+password.
- 셋업 관통: 모임 개설→①승인→멘티 신청→모집확정→②시작(IN_PROGRESS)까지 U1~U4 라우트로 구성.

## 커버 시나리오 (build-test-results.md에 실측)
- 시간창(창안 201/창전·창후 409), 멱등(재checkIn + DB rows=1), 비참여자 403, 80% 경계(4/5·3/5·5/5·S=0), ④ 확정 가드(COMPLETED/409/403), 완료 게이트 seam(U3←U5: 미종료 409·세션없음 200·전종료 200).
