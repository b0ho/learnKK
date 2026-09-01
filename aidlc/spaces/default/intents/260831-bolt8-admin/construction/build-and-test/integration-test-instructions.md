# Integration Test Instructions — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard(핵심 경계 통합). -->

## 프레임워크
- Testcontainers PostgreSQL 16 + Spring Boot Test + Flyway(V1~V10, 변경 없음) + `ddl-auto=validate`.

## 실행
- `cd backend && ./gradlew test`. 통합만 `--tests "com.learnkk.integration.*"`. Docker 필요.

## Bolt 8 판정
- 모니터링은 **read 전용 조합**이라 상태 전이·쓰기 경합이 없음 → 단위(집계식)+슬라이스(인가)로 충분 판정. 데이터 정합의 근거가 되는 쓰기 플로우는 기존 통합 테스트가 커버: MeetingIntegrationTest(승인 전이), EnrollmentIntegrationTest(신청/취소/재신청 — 본 Bolt에서 FR-12 현행화), SessionAttendanceIntegrationTest(세션→출석→수료).
- 후속 후보: 관리자 모니터링 관통 시나리오(개설→②→세션·출석→모니터링 조회로 출석율/수료 검증) — 파일럿 이후 추가 권장.

## 수동 라이브 시나리오 (보완)
1. `docker compose up -d db` → 백엔드 local 기동 → 관리자(V10 시드) 로그인.
2. `/admin/meetings`(승인 큐) → '운영 현황' 진입 → 전체 목록·상태 뱃지 확인.
3. 진행중 모임에서 출석 발생 후 재조회 → 출석율 변화 확인. 필터(모집중/진행중/완료) 전환 확인.
4. 멘토/멘티 토큰으로 `GET /api/admin/monitoring/meetings` 호출 → 403 MONITORING_FORBIDDEN 확인.
