# Code Summary — apply-button-state (bugfix)

브랜치: `bugfix/apply-button-state` (main에서 분기)

## 변경 요약

### Bug 1 — 내 신청 상태 반영 (FR-1)
- `frontend/src/features/meetings/MeetingListPage.tsx`: 로드 시 `role==='MENTEE'`이면 `enrollmentsApi.listMine()`로 본인 신청 내역을 조회해 `status==='APPLIED'` 모임을 `applied`에 병합. `Array.isArray` 가드로 목/구버전 응답 안전 처리, 조회 실패는 목록 렌더를 막지 않음. 방금 신청한 상태가 서버 스냅샷보다 우선(`{...mine, ...prev}`).
- 카드 버튼: `isApplied` → "신청완료" + 비활성.

### Bug 2 — 정원 마감 표기 (FR-2/FR-4)
- `backend/.../meeting/dto/MeetingSummary.java`: 컴포넌트 `enrolledCount`, `full` 추가. `from(Meeting, int enrolledCount)` 팩토리(`full = enrolledCount >= capacity`), 기존 `from(Meeting)`은 0/false로 위임.
- `backend/.../enrollment/repository/EnrollmentRepository.java`: `countByMeetingIdInAndStatusGrouped` — 모임 집합에 대한 APPLIED 인원 그룹 카운트(N+1 회피).
- `backend/.../enrollment/service/EnrollmentService.java`: `activeCountsByMeeting(Collection<Long>)` → `Map<Long,Integer>`.
- `backend/.../meeting/web/MeetingController.java`: 모집 목록(`list`)에서 `EnrollmentService`로 배치 카운트 조회 후 각 summary를 `enrolledCount`/`full`로 보강(`withEnrollmentCounts`). **경계**: 인원 집계를 서비스가 아닌 컨트롤러에서 조합 — `EnrollmentService→MeetingService` 의존이 이미 있어 역방향 주입은 빈 순환이 되고, ADR-007(meeting 서비스는 enrollment 테이블 직접 read 금지)을 지키기 위함.
- `contracts/openapi.yaml`: `MeetingSummary` 스키마에 `enrolledCount`(integer), `full`(boolean) 추가 + required 반영.
- `frontend/src/api/types.ts`: `MeetingSummary`에 `enrolledCount?`, `full?` 추가(optional 폴백).
- `MeetingListPage.tsx`: `!isApplied && meeting.full` → "마감" 배지(`full-badge-<id>`) + "마감" 라벨 비활성 버튼.

### 상태 우선순위 (FR-3)
신청완료 > 마감 > 신청. `isFull = !isApplied && full === true`로 신청완료가 마감보다 우선.

## 사전 존재 컴파일 에러 수리 (승인된 범위 확장)
main의 백엔드 테스트가 이미 컴파일 실패 상태였음(이전 인텐트에서 record 필드 추가 후 미갱신). 회귀 테스트 실행을 위해 함께 수리:
- `MeetingResponse` 13번째 인자 `mentorCompletionStatus` 누락 9곳 보정: EnrollmentServiceTest, PostServiceTest, ContentAccessServiceTest, MeetingControllerTest, PreSurveyServiceTest, FeedbackServiceTest, SessionServiceTest, AttendanceServiceTest, CompletionServiceTest.
- `MeetingSummary` 신규 arity(9-arg)로 갱신: MeetingControllerTest(2곳), OpenApiContractTest(2곳).

## 테스트 (신규/갱신)
- `MeetingControllerTest`: 모집 목록이 `enrolledCount`/`full`로 보강되어 나가는지(정원 5·신청 5 → full=true) 검증 + `@MockBean EnrollmentService` 추가.
- `EnrollmentServiceTest`: `activeCountsByMeeting` 그룹 행 매핑 / 빈 입력 시 미조회 검증.
- `MeetingListPage.test.tsx`: 로드 시 기존 신청 → "신청완료" 비활성 / `full:true` → "마감" 배지 + 비활성 버튼.

## 검증 결과
- 백엔드: `compileJava`/`compileTestJava` green. 대상 테스트(MeetingControllerTest, EnrollmentServiceTest, OpenApiContractTest) BUILD SUCCESSFUL.
- 프론트: `tsc --noEmit` 클린, 전체 vitest 28파일 137테스트 통과.
- 전체 백엔드 통합테스트(Testcontainers/Docker 의존)는 build-and-test 단계에서 수행.

## Story→Code 추적
- FR-1 → MeetingListPage.tsx (listMine effect, 버튼 라벨)
- FR-2 → MeetingSummary.java, EnrollmentRepository/Service, MeetingController, openapi.yaml, types.ts, MeetingListPage.tsx
- FR-3 → MeetingListPage.tsx (isFull 우선순위)
- FR-4 → MeetingSummary.java, openapi.yaml, types.ts

## 범위 외(유지)
관리자 큐/멘토 mine 목록의 인원 표기, 마감 필터/정렬, 실시간 갱신 — requirements Out of Scope. ci-pipeline/operation 미실행(project.md).
