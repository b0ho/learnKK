# Integration Test Instructions — Bolt 1 (learnKK)

<!-- build-and-test 산출물. Test Strategy=Standard(주요 경계·교차 상호작용). 출처: code-generation-plan.md(통합 테스트 단계)·code-summary.md(Testcontainers), team.md Testing Posture(Testcontainers 로컬 PostgreSQL·API 계약 테스트). -->

## 프레임워크·환경

- **Testcontainers**(PostgreSQL 16) + Spring Boot Test. 실 PostgreSQL 컨테이너에 Flyway 마이그레이션(V1~V3) 적용 후 `ddl-auto=validate`로 전 컨텍스트 기동 → 스키마·엔티티 매핑 실증.
- 싱글턴 컨테이너 패턴(컨텍스트 캐시 공유). 실행 `./gradlew test`(통합 포함).
- Rancher Desktop 환경변수(build-instructions.md 참조) 필요. Docker 미가용 시 통합 테스트만 스킵되고 단위 테스트는 실행됨.

## 통합 시나리오 (관통 경계)

- **AuthIntegrationTest**: 가입 → 로그인 → 세션 검증 end-to-end. 사번 unique 제약 경합 → 409.
- **MeetingIntegrationTest**: 모임 개설(PENDING_APPROVAL) → 관리자 ① 승인(RECRUITING) → listRecruiting 노출. 이중 승인 409, 비관리자 403.

## API 계약 테스트 (#1)

- **OpenApiContractTest**: swagger-parser로 `/contracts/openapi.yaml` 파싱 후 주요 엔드포인트 응답 DTO 직렬화 shape(required 필드 존재 + 미선언 필드 없음)를 스키마와 대조. 프론트-백엔드 interface 불일치 방지(team.md 최대 리스크).

## 데이터·환경 관리

- 컨테이너는 테스트 세션마다 초기화(재현성·독립성). 시드 데이터는 각 테스트가 준비(가입/개설 호출). 외부 SaaS 미사용(전부 로컬 제약 준수).
