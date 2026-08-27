# Code Generation Plan — apply-button-state (bugfix)

Unit: `apply-button-state` (bugfix, unit 미분할). 근거 요구사항: `inception/requirements-analysis/requirements.md` (FR-1~FR-4).

## 구현 개요
- **Bug 1 (FR-1)**: 모집 목록 로드 시 로그인 MENTEE의 신청 내역(`listMine`)을 반영해 이미 신청한 카드는 "신청완료"+비활성.
- **Bug 2 (FR-2)**: `MeetingSummary`에 `enrolledCount`/`full` 추가, 모집 목록 응답을 인원 수로 보강, FE는 마감 모임에 "마감" 배지+비활성 버튼.
- **경계 결정**: 인원 집계는 컨트롤러 계층에서 `EnrollmentService`와 조합(빈 순환 회피, ADR-007 경계 준수).

---

## Backend

- [ ] **Step 1 — `MeetingSummary` DTO 확장** (`meeting/dto/MeetingSummary.java`) — FR-2/FR-4
  - 컴포넌트 추가: `int enrolledCount`, `boolean full`.
  - 팩토리: `from(Meeting m, int enrolledCount)` → `full = enrolledCount >= capacity` 계산. 기존 `from(Meeting m)`은 `enrolledCount=0, full=false`로 위임(모집 목록 외 경로 호환).

- [ ] **Step 2 — 배치 카운트 쿼리** (`enrollment/repository/EnrollmentRepository.java`) — NFR(N+1 회피)
  - `@Query("SELECT e.meetingId, COUNT(e) FROM Enrollment e WHERE e.meetingId IN :ids AND e.status = :status GROUP BY e.meetingId")` → `List<Object[]>` (또는 프로젝션) 반환.

- [ ] **Step 3 — `EnrollmentService.activeCountsByMeeting`** (`enrollment/service/EnrollmentService.java`) — FR-4/Q4
  - `Map<Long,Integer> activeCountsByMeeting(Collection<Long> meetingIds)` — APPLIED 기준 집계, 빈 입력 시 빈 맵.

- [ ] **Step 4 — `MeetingController` 조합** (`meeting/web/MeetingController.java`) — FR-2
  - `EnrollmentService` 주입. `list()`(recruiting)에서 `listRecruiting` 결과의 meeting id 집합으로 `activeCountsByMeeting` 조회 후 각 summary를 `enrolledCount`로 보강한 새 `PageResponse` 반환.

- [ ] **Step 5 — OpenAPI 계약** (`contracts/openapi.yaml`) — FR-4
  - `MeetingSummary` 스키마에 `enrolledCount`(integer), `full`(boolean) 추가 및 `required`에 반영.

## Backend Tests

- [ ] **Step 6 — 회귀 테스트(신규)** — FR-2
  - `MeetingControllerTest`: 모집 목록 응답에 `enrolledCount`/`full`이 실려 나가는지, 컨트롤러가 카운트로 보강하는지 검증(서비스 목 + EnrollmentService 목).
  - `EnrollmentServiceTest`(또는 리포지토리 슬라이스): `activeCountsByMeeting`가 APPLIED만 집계하는지.
  - `OpenApiContractTest`: 갱신된 `MeetingSummary` 스키마 정합.

- [ ] **Step 7 — 사전 존재 컴파일 에러 수리(⚠ 범위 확인 대상)**
  - 현재 `main`은 백엔드 테스트가 컴파일되지 않음(11 errors, 이전 인텐트에서 record 필드 추가 후 미갱신):
    - `MeetingResponse` 13번째 인자 `mentorCompletionStatus` 누락 9곳(Enrollment/Post/ContentAccess/MeetingController/PreSurvey/Feedback/Session/Attendance/Completion ServiceTest).
    - `MeetingSummary` 6-arg 2곳(`MeetingControllerTest` L101/L230).
  - 회귀 테스트를 실행하려면 이 기계적 에러들을 함께 고쳐 suite를 green으로 되돌려야 함(각 호출에 누락 인자 append, MeetingSummary 호출은 신규 arity에 맞춤).

## Frontend

- [ ] **Step 8 — `MeetingSummary` 타입** (`frontend/src/api/types.ts`) — FR-4
  - `enrolledCount?: number; full?: boolean;` (optional — 목/구버전 응답 안전 폴백).

- [ ] **Step 9 — `MeetingListPage` 로직** (`frontend/src/features/meetings/MeetingListPage.tsx`) — FR-1/FR-2/FR-3
  - 로드 시 `role==='MENTEE'`이면 `enrollmentsApi.listMine()` 호출 → `Array.isArray` 가드 → `status==='APPLIED'` meetingId를 `applied`에 병합(post-apply 상태 우선). 실패는 목록 렌더 차단 안 함(FR-1).
  - 카드: `isApplied` → "신청완료"+비활성 / `!isApplied && meeting.full` → "마감"+비활성 + 헤더에 "마감" 배지(`data-testid=full-badge-<id>`) / 그 외 "신청". (우선순위 FR-3: 신청완료 > 마감 > 신청)

- [ ] **Step 10 — FE 테스트** (`frontend/src/features/meetings/MeetingListPage.test.tsx`) — FR-1/FR-2
  - 로드 시 `listMine`가 APPLIED 반환 → 해당 카드 버튼 "신청완료"+disabled.
  - `full:true` 모임 → "마감" 배지 + 버튼 disabled("마감").
  - 기존 테스트 호환 확인(listMine 목이 배열 아니면 무시되는지).

## 비적용(이 unit 범위 밖)
- 관리자 큐/멘토 mine 목록의 인원 표기, 마감 모임 필터/정렬, 실시간 갱신 — requirements Out of Scope.
- ci-pipeline/operation (project.md 스코프 오버라이드: build-and-test에서 종료).

## Story→Code 추적
- FR-1 → Step 8~10 (+9)
- FR-2 → Step 1~6, 8~10
- FR-3 → Step 9
- FR-4 → Step 1, 5, 8

## 테스트 전략
Minimal(bugfix): 요구사항당 회귀 테스트 중심 + 기존 suite green 복구. 신규 대량 테스트 지양.
