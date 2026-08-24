# 통합 테스트 지침 — learnKK ux-bugfixes

## 범위 (Minimal 전략)
Minimal 전략에서 신규 통합 테스트는 필수가 아니다. 다만 이 저장소에는 기존 통합 스위트(Testcontainers PostgreSQL)가 있으며, 이번 변경과 관련된 흐름은 아래로 커버된다.

## 실행 방법 (Testcontainers)
- `cd backend && ./gradlew test --tests "com.learnkk.integration.*"`
- 요구: 로컬 Docker 데몬 + Testcontainers가 접근 가능한 Docker 소켓.

## 관련 기존 통합 테스트
- `EnrollmentIntegrationTest.applyListCancelReapply_endToEnd` — 신청→취소→**재신청**(FR-12) 흐름.
- `SessionAttendanceIntegrationTest.sessionAttendanceCompletion_endToEnd` — 세션/출석/수료(FR-7/8 인접).
- `MeetingIntegrationTest.fullLifecycle...` — 승인 라이프사이클(FR-2/3/5 인접).

## 현재 환경 제약
이 실행 환경에서는 Testcontainers가 Docker 클라이언트 초기화에 실패한다(`DockerClientProviderStrategy` / `ExceptionInInitializerError`, JDK21 관련). 이 실패는 이번 변경과 무관하며(변경하지 않은 Messaging/Meeting 통합 테스트도 동일 실패), 회귀 신호가 아니다. 자세한 내용은 `build-test-results.md` 참조. Docker 클라이언트가 정상인 환경에서 재실행하면 된다.
