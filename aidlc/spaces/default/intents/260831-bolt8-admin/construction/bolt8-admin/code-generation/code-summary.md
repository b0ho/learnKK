# Code Summary — Bolt 8 Admin/Monitoring (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 8 = U9. Brownfield: 신규 admin 모듈(read 조합) + 시임 배선. git 브랜치 `bolt8/{사번}`. -->

## 목표 달성
관리자 운영 현황 모니터링(US-9.2): 전체 모임 목록·상태 + 모임별 출석율(세션 기준)·수료 진행을 read 전용 조합으로 제공. 승인 큐(US-9.1)는 Bolt 2 선구현분 그대로 사용 — 승인 액션 소유권 불변(ADR-007).

## 생성 파일 (신규 `com.learnkk.admin` 모듈)
- `admin/dto/MeetingMonitoringSummary.java` — 모니터링 행 record(상태·멘토 닉네임·멘티/세션·출석율·수료 카운트·멘토 수료 상태).
- `admin/service/AdminMonitoringService.java` — requireAdmin(403 MONITORING_FORBIDDEN) + U3/U4/U5/U2 read 조합. 출석율=총 출석/(S×멘티 수), 분모 0→0.0. `@Transactional(readOnly=true)`.
- `admin/web/AdminMonitoringController.java` — `GET /api/admin/monitoring/meetings`(status 선택 필터·페이징·sort 화이트리스트, 잘못된 status 400).
- FE: `features/meetings/AdminMonitoringPage.tsx`(상태 필터 + 모임 카드, read 전용).
- 테스트: `AdminMonitoringServiceTest`(4), `AdminMonitoringControllerTest`(5), `AdminMonitoringPage.test.tsx`(4), admin api 테스트(+2).

## 수정 파일
- `session/repository/AttendanceRepository.java` — `countByMeetingId`(모임 전체 출석 수, meeting_session join) 추가.
- `kernel/error/ErrorCodes.java` — `MONITORING_FORBIDDEN`(U9 도메인 섹션 신설).
- `contracts/openapi.yaml` — 모니터링 path + `MeetingMonitoringSummary`/`PageMeetingMonitoringSummary` 스키마(출석율 정의 명문화).
- FE: `api/{types,admin}.ts`(+`listMonitoring`), `routes/{paths,AppRouter}`(ADMIN 라우트 `/admin/monitoring`), `AdminApprovalPage.tsx`(운영 현황 진입 버튼).

## 사전 빌드 정리 (기존 결함 — 본 Bolt에서 발견·수정)
- **구버전 record 생성자**: FR-7 `mentorCompletionStatus` 추가 이후 미갱신된 테스트 호출 11곳(MeetingResponse 9·MeetingSummary 2) — 8개 테스트 파일에 마지막 인자 보강(OpenApiContractTest만 기갱신 상태였음).
- **EnrollmentIntegrationTest**: FR-12(취소 행 재사용 재신청) 도입 이후 미갱신 — 재신청 기대값 `ENROLLMENT_DUPLICATE`→`ENROLLMENT_FULL`(정원 선점) 수정, APPLIED 중복 재신청 검증 블록 추가로 원 의도 커버리지 유지.
- **AppShell.tsx**: `TAB_ROOTS: readonly string[]` 명시 — `as const` PATHS 리터럴 유니온과 `includes(string)` 충돌로 인한 tsc 빌드 에러 해소.

## 주요 구현 결정
- **read 조합(ADR-007)**: U9 서비스는 타 모듈 테이블 직접 접근 없이 repository/service read만 주입(EnrollmentService.listActiveMenteeIds, userRepository→nickname은 기존 선례). 라우트도 `/api/admin/monitoring`으로 분리해 승인 액션(`/api/admin/meetings`)과 경계 유지.
- **출석율(세션 기준)**: 멘티별 a/S(BR-U5-3)의 모임 평균과 동치인 총출석/(S×멘티수) 채택 — 분모를 S(전체 예정)로 두어 진행 중 세션 출석으로 1.0 초과가 없음. endedSessionCount는 별도 참고 지표.
- **DB 변경 없음**: 마이그레이션 추가 없음(V1~V10 불변) — 조회 전용 계층.
- **N+1 허용**: 페이지 크기 [1,100] 클램프 + 조회 전용 관리자 화면이라 행당 소수 read 허용(대량 시 배치 read 후속 검토, performance-test-instructions 기록).

## 검증 결과
- 백엔드: `./gradlew build` 컴파일·전체 테스트 321개 통과(사전 빌드 정리 후 로컬 그린 — build-test-results.md).
- 프론트엔드: `tsc -b && vite build` 타입 에러 0(AppShell 수정 후), Vitest 신규 6케이스 포함 통과.
- 계약: openapi.yaml YAML 파싱·스키마 참조 정합 확인.

## 범위 밖 / 후속
- US-9.3 집계 지표(FR9.2 TBD). 모니터링 정렬 UI. 대량 모임 시 배치 read 최적화.
