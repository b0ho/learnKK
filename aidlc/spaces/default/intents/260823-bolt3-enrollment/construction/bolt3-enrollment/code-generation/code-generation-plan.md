# Code Generation Plan — Bolt 3 Enrollment (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 3 = U4 Enrollment(선착순 신청·정원/중복 제어·취소·신청자 목록·멘티 현황). Brownfield: Bolt 1/2 코드에 신규 enrollment 모듈 추가 + 시임 배선. 상속 설계: 260731-learnkk-crew intent U4 functional-design(business-logic-model/business-rules/domain-entities)·nfr-requirements. 규칙: team.md(monorepo·3계층·계약우선·test-alongside·80% floor)·project.md(스택 lock·camelCase/snake_case·전역 에러). -->

## 목표 (Definition of Done — bolt-plan Bolt 3)

> 멘티 신청 → 현황 확인 → 취소. 선착순 정원·중복 제어·신청자 목록.

- **불변식(BR-U4-1): overbooking 금지** — 활성(APPLIED) 신청 수 ≤ 모임 capacity. 잔여 1석 동시 신청 시 1건만 성공, 나머지 409.
- 중복 신청 차단, ②시작 이후 취소 불가, 취소 시 빈자리 복귀.
- 확신 가설(bolt-plan): 잔여 1석 동시 신청·중복 신청 경계가 정원 무결성을 지킨다.
- test-alongside, BE/FE 각 80% line floor + 동시성/경계 시나리오.

## 상속·통합 지점 (기존 코드)

- **U4→U3 read**: `MeetingService.getMeeting(id)` → MeetingResponse(capacity·status·mentorId)로 신청 가능(RECRUITING)·정원 판정. U3 meeting 테이블 직접 접근 금지(MeetingService 경유).
- **재사용**: kernel 에러 계층(Conflict/Forbidden/NotFound/Validation), Principal/@AuthPrincipal, PageResponse/PageRequestFactory, SessionAuthInterceptor.
- **시임 배선**: Bolt 2가 남긴 U3 운영 허브 신청자 placeholder(MentorHub) → listApplicants 연결. 멘티 현황(MyLearningPage MENTEE placeholder) → listMyEnrollments 대체.

## 범위 밖 (이월)

- 멘티 현황의 세션 일정(U5, Bolt 6) 조합 — placeholder. 대기열(waitlist)·취소 후 재신청 — 설계상 없음.

---

## 실행 단계 (layer-by-layer)

### Step 1: DB 스키마 — V4 마이그레이션
- [x] `backend/src/main/resources/db/migration/V4__enrollment.sql` — `enrollment`(id BIGINT identity PK, meeting_id FK→meetings ON DELETE CASCADE, mentee_id FK→users ON DELETE CASCADE, status varchar(20) NOT NULL CHECK IN('APPLIED','CANCELLED'), applied_at timestamptz NOT NULL DEFAULT now(), cancelled_at timestamptz, created_at/updated_at). **UNIQUE(meeting_id, mentee_id)**(중복 백스톱). 인덱스 idx_enrollment_meeting_status(meeting_id,status), idx_enrollment_mentee(mentee_id).
- 추적: domain-entities Enrollment, BR-U4-2(unique), V1~V3 규약

### Step 2: 도메인 — Entity + enum + Repository
- [x] `enrollment/domain/EnrollmentStatus.java`(U4 로컬 enum: APPLIED, CANCELLED).
- [x] `enrollment/entity/Enrollment.java`(@Entity meetings/users FK by id, @Enumerated STRING, appliedAt/cancelledAt).
- [x] `enrollment/repository/EnrollmentRepository.java`: `int countByMeetingIdAndStatus(Long, EnrollmentStatus)`, `Optional<Enrollment> findByMeetingIdAndMenteeId(Long,Long)`, `List<Enrollment> findByMeetingIdAndStatus(Long, EnrollmentStatus)`, `List<Enrollment> findByMenteeIdOrderByAppliedAtDesc(Long)`, + 어드바이저리 락 네이티브 쿼리 `@Query(value="SELECT pg_advisory_xact_lock(:key)", nativeQuery=true) void lockMeeting(@Param("key") long key)`.
- 추적: domain-entities, component-methods C3

### Step 3: kernel — 에러 코드 + Principal.isMentee()
- [x] `kernel/error/ErrorCodes.java`에 `// --- Enrollment domain ---`: ENROLLMENT_NOT_FOUND, ENROLLMENT_FORBIDDEN, ENROLLMENT_FULL, ENROLLMENT_DUPLICATE, ENROLLMENT_NOT_OPEN, ENROLLMENT_CANCEL_FORBIDDEN.
- [x] `kernel/security/Principal.java`에 `isMentee()` 추가(isMentor/isAdmin 미러).
- 추적: BR-U4-1~5 CC-1 매핑, 인가

### Step 4: EnrollmentService (C3) — 신청/취소/목록
- [x] 의존: EnrollmentRepository + MeetingService(read).
- [x] `apply(Principal, meetingId)` — MENTEE 403(isMentee), getMeeting 404, status!=RECRUITING→409 ENROLLMENT_NOT_OPEN. **원자 구간**: `lockMeeting(meetingId)` → count(APPLIED) ≥ capacity → 409 ENROLLMENT_FULL → insert(APPLIED). unique 위반(동시 중복)→409 ENROLLMENT_DUPLICATE(DataIntegrityViolation catch 또는 선검증).
- [x] `cancel(Principal, meetingId)` — 본인 신청 조회 404, 없으면 404. getMeeting status ∈{RECRUITING,READY_TO_START}이어야(그 외 409 ENROLLMENT_CANCEL_FORBIDDEN). status=CANCELLED, cancelledAt.
- [x] `listApplicants(Principal, meetingId)` — getMeeting로 소유 멘토(mentorId==userId) 또는 ADMIN 확인(403). APPLIED 목록 + 멘티 닉네임(U2 read: UserRepository.findById, U4→U2 허용) → ApplicantResponse[].
- [x] `listMyEnrollments(Principal)` — 본인 APPLIED/CANCELLED 목록(meetingId·status·appliedAt) → EnrollmentResponse[]. (모임 정보는 FE getMeeting 조합.)
- 추적: W1~W4, BR-U4-1~5, business-logic-model TOCTOU 주석(원자성은 락 보장)

### Step 5: DTO + Controller + 인터셉터
- [x] DTO: `EnrollmentResponse`(id,meetingId,menteeId,status,appliedAt), `ApplicantResponse`(menteeId,nickname,appliedAt).
- [x] `enrollment/web/EnrollmentController.java`: `POST /api/meetings/{id}/enrollments`(apply, 201), `DELETE /api/meetings/{id}/enrollments/mine`(cancel, 204), `GET /api/meetings/{id}/applicants`(listApplicants), `GET /api/enrollments/mine`(listMyEnrollments).
- [x] `SessionAuthInterceptor.isProtected` 확장: 위 4개 라우트 전부 protected(인증 필요). id-bearing 경로는 Pattern 상수 추가(MEETING_QUESTIONS 방식).
- 추적: component-methods, 계약 #1, SessionAuthInterceptor

### Step 6: 백엔드 테스트 (Standard)
- [x] `EnrollmentServiceTest`(Mockito): apply 정상·비멘티403·비RECRUITING409·정원마감409·중복409, cancel 정상·②후409·비본인404, listApplicants 소유자외403.
- [x] `EnrollmentConcurrencyIntegrationTest`(Testcontainers): capacity=1에 N-thread 동시 apply → 정확히 1 APPLIED + 나머지 ENROLLMENT_FULL; 동시 중복 apply → 1건.
- [x] `EnrollmentControllerTest`(@WebMvcTest, @MockBean AuthService): 라우트 상태코드·인가.
- [x] `integration/EnrollmentIntegrationTest`: 신청→listApplicants→취소→빈자리 재신청 end-to-end.
- 추적: team.md Testing Posture, BR-U4-1(무결성 필수)

### Step 7: 계약 #1 — openapi.yaml
- [x] version bump(`0.2.0-bolt2`→`0.3.0-bolt3`), tag `enrollments` 추가.
- [x] paths: `/api/meetings/{id}/enrollments`(POST), `/api/meetings/{id}/enrollments/mine`(DELETE), `/api/meetings/{id}/applicants`(GET), `/api/enrollments/mine`(GET). schemas: EnrollmentResponse, ApplicantResponse. 409(FULL/DUPLICATE/NOT_OPEN/CANCEL_FORBIDDEN) 응답.
- [x] `OpenApiContractTest` 신규 DTO 정합 확장.

### Step 8: Frontend API 계층
- [x] `api/enrollments.ts`: `apply(meetingId)`, `cancel(meetingId)`(DELETE), `listMine()`, `listApplicants(meetingId)`.
- [x] `api/types.ts`: EnrollmentResponse·ApplicantResponse·EnrollmentStatus. `api/index.ts` re-export.

### Step 9: Frontend 멘티 신청 (MeetingListPage)
- [x] RECRUITING 카드에 role===MENTEE일 때 "신청" 버튼(인라인, 상세 페이지 부재). 신청 성공/409(마감·중복·모집아님) 한국어 매핑. `data-testid`.
- 추적: enrollment feature 신청 화면, CC-2/CC-3

### Step 10: Frontend 멘티 현황 (MyLearningPage MENTEE)
- [x] MENTEE placeholder 제거 → listMine 기반 내 신청 목록(모임 상태·appliedAt). 각 항목 모임 정보는 getMeeting 조합. 취소 버튼(모임 status∈{RECRUITING,READY_TO_START}일 때만 노출, 서버 재검증). 세션 일정(U5) placeholder 유지.
- 추적: W4, BR-U4-3

### Step 11: Frontend 멘토 허브 신청자 배선 (MentorHub)
- [x] MentorHub 각 모임 카드에 신청자 목록/수(listApplicants) 표시 — Bolt 2 placeholder note 대체. (사전설문 응답 U8/Bolt7은 placeholder 유지.)
- 추적: US-2.3, BR-U4-4, ADR-007 R-1

### Step 12: Frontend 테스트
- [x] MeetingListPage 신청 버튼·409 매핑(MENTEE), MyLearningPage 멘티 현황·취소, MentorHub 신청자 표시, enrollments api 단위.

### Step 13: 문서
- [x] README에 Bolt 3 범위(신청/정원/취소) + 엔드포인트 반영.

---

## Assumptions

- 정원 동시성 = 어드바이저리 락(`pg_advisory_xact_lock`) + count + unique 백스톱. overbooking 금지 필수.
- U4→U3 read는 MeetingService.getMeeting 경유(모듈 소유 규칙). U4→U2(닉네임)는 UserRepository read.
- 취소 후 재신청 불가(unique 유지), 대기열 없음.
- 멘티 현황 세션 일정(U5)·사전설문(U8)은 미구현 → placeholder.
- ci-pipeline·operation은 project.md Scope Override로 build-and-test 이후 SKIP.

## 테스트 전략 (Standard)

- 컴포넌트당 5~8 단위 + 동시성/관통 통합. 정원 무결성(잔여1석 경합·중복)은 통합 테스트로 필수 커버. 이 환경 Testcontainers 미가용 시 라이브 API 병렬 E2E로 보완.
