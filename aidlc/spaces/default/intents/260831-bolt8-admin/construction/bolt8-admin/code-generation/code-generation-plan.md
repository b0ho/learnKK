# Code Generation Plan — Bolt 8 Admin/Monitoring (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 8 = U9 Admin/Monitoring. Brownfield: 신규 admin 모듈(read 조합) + 시임 배선. 상속 설계: 260731-learnkk-crew intent U9(unit-of-work·ADR-007)·bolt-plan Bolt 8. 규칙: team.md·project.md·construction.md. -->

## 목표 (Definition of Done — bolt-plan Bolt 8)

> 승인 큐(①②③④+모집확정 대기) 조회 · 운영 현황 모니터링(세션 기준 출석율).

- **승인 큐(US-9.1)**: Bolt 2에서 선구현 완료(`/api/admin/meetings` listByStatus + AdminApprovalPage) — 본 Bolt 범위 아님(재확인만).
- **운영 현황 모니터링(US-9.2)**: 전체 모임 목록·상태 + 모임별 **출석율(세션 기준)** + **수료 진행**을 관리자 전용 read 조합으로 제공.
- 확신 가설(bolt-plan): 관리자 조회 계층이 각 도메인 read 조합으로 현황·큐를 정확히 집계(집계 지표는 범위 밖 = US-9.3 Won't).
- test-alongside, BE/FE 각 80% floor + 인가·0나눗셈 경계 시나리오.

## 상속·통합 지점 (기존 코드)

- **ADR-007 (read 조합)**: U9는 승인 액션을 소유하지 않음(U3/U5 유지). 조회 전용 서비스가 U3(Meeting)·U4(Enrollment)·U5(Session/Attendance/Completion)·U2(User 닉네임) read만 조합.
- **재사용**: kernel(Principal, PageResponse, PageRequestFactory, 에러 계층), `MeetingRepository.findByStatus/findAll`, `MeetingSessionRepository.findByMeetingIdOrderByWeekAscScheduledAtAsc`(+`isEnded(now)`), `MenteeCompletionRepository.findByMeetingId`, `EnrollmentService.listActiveMenteeIds`(U9→U4 read 선례), `userRepository.findById→nickname`(EnrollmentService.resolveNickname 선례).
- **출석율 정의(세션 기준)**: 멘티별 a/S(BR-U5-3)의 모임 평균 = `총 출석 수 / (전체 예정 세션 수 S × 참여 멘티 수)`, 분모 0이면 0.0(AttendanceSummaryResponse 0나눗셈 회피 선례).
- **gotcha**: Attendance에는 모임 단위 합계 쿼리가 없음 → `AttendanceRepository.countByMeetingId`(meeting_session join) 추가 필요.
- **시임 배선**: AdminApprovalPage 헤더 ↔ AdminMonitoringPage 상호 진입 버튼. 관리자 탭 루트는 승인 큐 유지.

## 범위 밖 (이월)
- US-9.3 집계 지표(개설 대비 승인률·평균 출석율·만족도 등) — FR9.2 TBD(Won't).
- 모니터링 정렬 UI(백엔드 sort=id/createdAt/title 지원, FE 노출은 후속).

---

## 실행 단계 (layer-by-layer)

### Step 1: U5 read 포트 보강
- [x] `session/repository/AttendanceRepository.java` — `countByMeetingId(meetingId)`(Attendance×MeetingSession join COUNT) 추가. 기존 `countAttendedSessions` 패턴 상속.
- 추적: BR-U5-3, US-9.2 분자

### Step 2: kernel — 에러 코드
- [x] `kernel/error/ErrorCodes.java` `// --- Admin / monitoring domain (U9) ---`: `MONITORING_FORBIDDEN`.
- 추적: CC-1 매핑

### Step 3: 신규 admin 모듈 (C8, read 조합)
- [x] `admin/dto/MeetingMonitoringSummary.java` — id·title·status·mentorId·mentorNickname·menteeCount·sessionCount·endedSessionCount·attendanceRate·completedMenteeCount·completionCandidateCount·mentorCompletionStatus.
- [x] `admin/service/AdminMonitoringService.java` — `listMeetings(principal, status?, pageable)`: requireAdmin(403 MONITORING_FORBIDDEN) → status 유무로 findByStatus/findAll → 모임별 세션(종료=isEnded(now))·멘티 수·출석율·수료 카운트(COMPLETED/CANDIDATE)·멘토 닉네임 조합. `@Transactional(readOnly=true)`. 페이지 크기 [1,100] 클램프라 행당 소수 read 허용(M 복잡도).
- [x] `admin/web/AdminMonitoringController.java` — `GET /api/admin/monitoring/meetings?status&page&size&sort`(SORTABLE=id/createdAt/title, 잘못된 status 400 VALIDATION_FAILED). 승인 액션 라우트와 분리(`/api/admin/meetings` 불변).
- 추적: unit-of-work U9, ADR-007, MeetingApprovalController 선례

### Step 4: 백엔드 테스트 (Standard)
- [x] `AdminMonitoringServiceTest`(Mockito) — 출석율 3/(2×2)=0.75·수료 카운트 조합, 세션/멘티 0 → rate 0.0, status 필터 위임, 비관리자(MENTOR/MENTEE) 403.
- [x] `AdminMonitoringControllerTest`(@WebMvcTest, @MockBean AuthService) — 관리자 200(조합 행 직렬화), status 필터 파싱, 잘못된 status 400, 비관리자 403(MONITORING_FORBIDDEN), 미인증 401.
- 추적: team.md Testing Posture

### Step 5: 계약 #1 — openapi.yaml
- [x] `/api/admin/monitoring/meetings` path(admin tag) + `MeetingMonitoringSummary`/`PageMeetingMonitoringSummary` 스키마. 출석율 정의·분모 0 규칙을 description에 명문화.

### Step 6: Frontend API + 타입
- [x] `api/types.ts` `MeetingMonitoringSummary`, `api/admin.ts` `listMonitoring({status?, page?, size?})`.

### Step 7: Frontend 관리자 화면
- [x] `features/meetings/AdminMonitoringPage.tsx` — 상태 필터(전체/모집중/시작대기/진행중/완료) + 모임 카드(상태 뱃지·멘토 닉네임·멘티/세션 종료 현황·출석율 formatRate·수료 확정/후보). read 전용 — 액션은 승인 큐로.
- [x] `routes/paths.ts` `adminMonitoring`, `AppRouter` `<RequireRole allow={['ADMIN']}/>` 라우트, AdminApprovalPage ↔ AdminMonitoringPage 상호 진입 버튼.
- 추적: US-9.2 인수기준, CC-3

### Step 8: Frontend 테스트
- [x] `AdminMonitoringPage.test.tsx` — 조합 행 렌더(출석율 85%·수료 카운트, 카드 스코프 within), 필터 클릭 시 status 쿼리 재조회, 빈 목록, API 에러 노출.
- [x] `admin.test.ts` — listMonitoring 라우트/쿼리 직렬화(기본 status 미포함).

### Step 9: 사전 빌드 정리 (본 Bolt에서 발견된 기존 결함)
- [x] FR-7 `mentorCompletionStatus` 필드 추가 이후 미갱신된 구버전 record 생성자 호출 정리 — MeetingResponse 9곳·MeetingSummary 2곳(Enrollment/Post/ContentAccess/Session/Attendance/Completion/PreSurvey/FeedbackServiceTest, MeetingControllerTest).
- [x] `EnrollmentIntegrationTest` — FR-12(취소 후 재신청 허용) 도입 이후 미갱신된 기대값 수정(재신청→ENROLLMENT_FULL) + APPLIED 중복 재신청(ENROLLMENT_DUPLICATE) 검증 블록 추가.
- [x] `AppShell.tsx` `TAB_ROOTS: readonly string[]` 타입 명시 — `as const` PATHS 리터럴 유니온으로 인한 `includes(string)` 빌드 에러 수정.

---

## Assumptions
- 출석율(세션 기준)=총 출석 수/(S×멘티 수) — 멘티별 a/S(BR-U5-3) 정의의 모임 평균과 동치. 분모 0이면 0.0.
- 세션 '종료'=수동 완료 또는 시간창 경과(`MeetingSession.isEnded`, FR-8) — endedSessionCount는 참고 지표, 출석율 분모는 S(전체 예정) 유지.
- 멘토 닉네임은 최소 U2 read(null 허용 — FE는 `#mentorId` 폴백).
- DB 변경 없음(read 전용) — 마이그레이션·시드 불변(V1~V10).
- ci-pipeline·operation은 project.md Scope Override로 build-and-test 이후 SKIP.

## 테스트 전략 (Standard)
- 서비스 단위(집계식·경계) + 컨트롤러 슬라이스(인가·파싱) + FE 컴포넌트/단위. 통합은 기존 관리자 플로우 통합 테스트가 승인 큐를 커버 — 모니터링은 read 전용이라 단위/슬라이스로 충분 판단, 라이브 확인은 수동 시나리오로 보완.
