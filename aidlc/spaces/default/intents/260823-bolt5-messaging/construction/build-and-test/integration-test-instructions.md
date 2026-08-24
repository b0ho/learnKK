# Integration Test Instructions — Bolt 5 Messaging (Standard)

<!-- 상류: bolt5-messaging code-generation-plan.md (Step 7) · code-summary.md. -->

## 범위·프레임워크

- `MessageIntegrationTest`(`backend/src/test/java/com/learnkk/integration/`) — `@SpringBootTest` + MockMvc + Testcontainers(PostgreSQL 16), Flyway V1~V5, 전체 컨텍스트(인터셉터+advice).
- 경계 관통: 멘토·멘티(자기 모임 활성 신청) 양방향 발신→동일 스레드 수렴, 열람 시 미확인 0(멱등), 무관 상대 403, 관리자 200, 자기발신 400.

## 실행 방법

- 요구: Docker 접근 가능 데몬(Testcontainers).
- `cd backend && ./gradlew test --tests "com.learnkk.integration.MessageIntegrationTest"`

## 환경·데이터

- 컨테이너: `postgres:16` 싱글턴(AbstractIntegrationTest). 사용자/모임/신청은 실제 API(`/api/auth`, `/api/meetings`, `/api/meetings/{id}/enrollments`)로 시드. 관리자는 UserRepository로 직접 시드 후 로그인.

## 이 환경에서의 상태

- **미실행**: Windows/Rancher Desktop docker-java JNA 초기화 실패(Bolt 1~3 동일). 코드 결함 아님.
- 대체 검증: 권한 경계(403/400/401) 가설은 `MessageControllerTest`(전체 MockMvc 체인)와 `MessageServiceTest`로 로컬 실증. Docker 가용 환경/CI에서 통합 테스트 실행 권장.
