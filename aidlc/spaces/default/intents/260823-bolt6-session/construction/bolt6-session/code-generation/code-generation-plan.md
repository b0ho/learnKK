# Code Generation Plan — Bolt 6 Session/Attendance (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 6 = U5 Session/Attendance(세션 일정·팝업 출석(시간창)·출석율·80% 수료 자동판정·④ 확정). Brownfield: Bolt 1/2/3 코드에 신규 session 모듈 추가 + SessionCompletionGate 시임 배선. 상속 설계: 260731-learnkk-crew intent U5 functional-design(business-logic-model/business-rules/domain-entities)·nfr-requirements. 규칙: team.md(monorepo·3계층·계약우선·test-alongside·80% floor)·project.md(스택 lock·camelCase/snake_case·전역 에러·DTO 경계). -->

## 목표 (Definition of Done — bolt-plan Bolt 6)

> 세션 생성 → 예정 시각 출석 팝업 → 체크 → 출석율/수료 판정.

- 세션 일정 지정·변경(주차당 복수), 멘티 팝업 출석(시간창), 출석율(세션 기준), 80% 자동판정, ④ 관리자 확정.
- **확신 가설(bolt-plan):** 스케줄러리스 시간 판정이 세션창 개폐·출석 멱등·80% 경계(`a*100 >= 80*S`)를 정확히 처리한다.
- test-alongside, BE/FE 각 80% line floor + 시간창/멱등/80%경계/S=0 시나리오.

## 상속·통합 지점 (기존 코드)

- **U5→U3 read:** `MeetingService.getMeeting(id)` → MeetingResponse(status·mentorId)로 소유 멘토(세션 관리 권한)·모임 IN_PROGRESS(출석 활성) 판정. U3 meeting 테이블 직접 접근 금지.
- **U5→U4 read (신규 포트):** 참여자(APPLIED 멘티) 집합·참여 여부는 `EnrollmentService`에 무권한 read 포트를 신설해 경유. (리뷰 S3 해소.)
- **U3←U5 seam 교체:** 기존 `meeting/service/NoSessionsCompletionGate`(항상 true 스텁)를 실제 세션 read 기반 구현으로 교체. `SessionCompletionGate.allScheduledSessionsEnded(meetingId)` 계약 유지, 상태 쓰기(COMPLETED)는 U3 소유(`MeetingApprovalService.completeMeeting`) 유지. (ADR-007 R-2.)
- **재사용:** kernel 에러 계층(Conflict/Forbidden/NotFound/Validation), `CompletionStatus`(U1 enum), Principal/@AuthPrincipal, PageResponse, SessionAuthInterceptor.
- **테이블명:** auth 토큰 테이블 `sessions`(V2)와 충돌 회피 → U5 세션 테이블은 `meeting_session`. 엔티티는 `com.learnkk.session.entity.Session`(패키지 격리) + `@Table(name="meeting_session")`.

## 범위 밖 (이월)

- 사전설문 응답(U8/Bolt 7)·쪽지(U7/Bolt 5)·게시글(U6/Bolt 4). 관리자 종합 모니터링 집계(U9/Bolt 8). 세션 변경 푸시 통지(A6는 인앱 현황 갱신으로 처리).

---

## 실행 단계 (layer-by-layer)

### Step 1: DB 스키마 — V5 마이그레이션
- [x] `backend/src/main/resources/db/migration/V5__session.sql`:
  - `meeting_session`(id identity PK, meeting_id FK→meetings ON DELETE CASCADE NOT NULL, week int NOT NULL, scheduled_at timestamptz NOT NULL, check_in_window_minutes int NOT NULL DEFAULT 120, created_at/updated_at). 인덱스 `idx_meeting_session_meeting(meeting_id)`.
  - `attendance`(id identity PK, session_id FK→meeting_session ON DELETE CASCADE NOT NULL, mentee_id FK→users ON DELETE CASCADE NOT NULL, checked_in_at timestamptz NOT NULL DEFAULT now(), created_at). **UNIQUE(session_id, mentee_id)** = 멱등 백스톱(BR-U5-2). 인덱스 `idx_attendance_mentee(mentee_id)`.
  - `mentee_completion`(meeting_id BIGINT NOT NULL, mentee_id BIGINT NOT NULL, status varchar(30) NOT NULL DEFAULT 'NOT_COMPLETED' CHECK IN('NOT_COMPLETED','COMPLETION_CANDIDATE','COMPLETED'), attended_count int NOT NULL DEFAULT 0, total_scheduled int NOT NULL DEFAULT 0, approved_at timestamptz, created_at/updated_at, **PRIMARY KEY(meeting_id, mentee_id)**, FK meeting_id→meetings CASCADE, mentee_id→users CASCADE).
- 추적: domain-entities(Session/Attendance/MenteeCompletion), V1~V4 규약.

### Step 2: 도메인 — Entity + Repository
- [x] `session/entity/Session.java`(@Entity @Table(name="meeting_session"); meetingId/week/scheduledAt/checkInWindowMinutes; `windowEnd()` = scheduledAt+minutes 헬퍼; `reschedule(newAt)`).
- [x] `session/entity/Attendance.java`(@Entity; sessionId/menteeId/checkedInAt).
- [x] `session/entity/MenteeCompletion.java`(@Entity @IdClass 또는 @Embeddable 복합키(meetingId,menteeId); status @Enumerated(STRING) CompletionStatus; attendedCount/totalScheduled/approvedAt; `markCandidate(a,S)`/`markNotCompleted(a,S)`/`approve(now)`).
- [x] `session/repository/SessionRepository.java`: `List<Session> findByMeetingIdOrderByWeekAscScheduledAtAsc(Long)`, `int countByMeetingId(Long)`, `Optional<Session> findById(Long)`.
- [x] `session/repository/AttendanceRepository.java`: `Optional<Attendance> findBySessionIdAndMenteeId(Long,Long)`, `boolean existsBySessionIdAndMenteeId(Long,Long)`, `long countBySessionIdInAndMenteeId(Collection<Long>,Long)` 또는 `@Query`로 `countAttendedSessions(meetingId, menteeId)`(attendance join meeting_session where meetingId).
- [x] `session/repository/MenteeCompletionRepository.java`: `Optional<MenteeCompletion> findByMeetingIdAndMenteeId(Long,Long)`, `List<MenteeCompletion> findByMeetingId(Long)`.
- 추적: domain-entities, component-methods C4.

### Step 3: kernel — 에러 코드
- [x] `kernel/error/ErrorCodes.java`에 `// --- Session domain ---`: `SESSION_NOT_FOUND`, `SESSION_FORBIDDEN`, `SESSION_MEETING_NOT_ACTIVE`, `ATTENDANCE_WINDOW_CLOSED`, `ATTENDANCE_NOT_PARTICIPANT`, `COMPLETION_NOT_ELIGIBLE`, `COMPLETION_ALREADY_APPROVED`.
- 추적: BR-U5-2/4/5/6, U1 CC-1(코드 `<DOMAIN>_<REASON>`, 메시지 한국어).

### Step 4: U4 참여자 read 포트 신설 (리뷰 S3)
- [x] `EnrollmentService`에 무권한 cross-module read 포트 추가:
  - `List<Long> listActiveMenteeIds(Long meetingId)` — APPLIED 멘티 id 목록(computeCompletion 대상).
  - `boolean isActiveParticipant(Long meetingId, Long menteeId)` — checkIn 참여자 게이트.
- [x] `EnrollmentRepository`에 필요한 조회(`existsByMeetingIdAndMenteeIdAndStatus`) 추가 가능.
- 추적: domain-entities(수료 대상=U4 read), ADR-007 R-1, DAG U5→U4 정방향.

### Step 5: SessionService (C4) — 세션 일정 + read-out
- [x] 의존: SessionRepository + MeetingService(read).
- [x] `addSession(Principal, meetingId, {week, scheduledAt, checkInWindowMinutes?})` — 소유 멘토(getMeeting mentorId==userId, isMentor) 아니면 403 SESSION_FORBIDDEN; 모임 status=IN_PROGRESS 아니면 409 SESSION_MEETING_NOT_ACTIVE [assumption]; Session insert.
- [x] `updateSession(Principal, sessionId, {scheduledAt})` — 소유 멘토 검증 → scheduledAt 갱신(멘티 현황 반영).
- [x] `listSessions(meetingId)` — 세션 목록(week·scheduledAt·window). 멘티/멘토/관리자 read.
- [x] read-out `allScheduledSessionsEnded(meetingId)` — 모든 세션 `scheduledAt+window < now`(세션 없으면 vacuous-true). SessionCompletionGate 구현이 사용.
- 추적: W1, BR-U5-1, read-out(U3 ③ 전제).

### Step 6: AttendanceService (C4) — 스케줄러리스 시간창 출석
- [x] 의존: AttendanceRepository + SessionRepository + MeetingService(read) + EnrollmentService(참여자 read).
- [x] `checkIn(Principal, sessionId)` — 세션 404; `isActiveParticipant` 아니면 403 ATTENDANCE_NOT_PARTICIPANT; 모임 IN_PROGRESS 아니면 409 SESSION_MEETING_NOT_ACTIVE; `now ∈ [scheduledAt, scheduledAt+window]` 아니면 409 ATTENDANCE_WINDOW_CLOSED; Attendance upsert(멱등 — unique 위반은 기존 유지·무해).
- [x] `getMyAttendance(Principal, meetingId)` — a=출석 세션 수, S=전체 예정 세션 수; `rate = (S>0)? a/S : 0`(리뷰 S1, 0나눗셈 회피) → `{attended, totalScheduled, rate}`.
- 추적: W2, BR-U5-2/3, ADR-005 요청시점 판정.

### Step 7: CompletionService (C4) — 80% 자동판정 + ④ 확정
- [x] 의존: MenteeCompletionRepository + SessionRepository + AttendanceRepository + EnrollmentService(참여자) + MeetingService(read).
- [x] `computeCompletion(Principal, meetingId)` — 소유 멘토/관리자 조회 권한(403); participants=`listActiveMenteeIds`; S=`countByMeetingId`; 각 멘티 a 집계 후 `if S>0 && a*100 >= 80*S` → COMPLETION_CANDIDATE else NOT_COMPLETED; mentee_completion upsert(a/S 스냅샷). MenteeCompletion[] 반환.
- [x] `getCompletions(Principal, meetingId)` — 소유 멘토/관리자, mentee_completion 목록 조회.
- [x] `approveMenteeCompletion(Principal admin, meetingId, menteeId)` — role=ADMIN 아니면 403; mc 404; **COMPLETED면 409 COMPLETION_ALREADY_APPROVED(먼저 검사)**; COMPLETION_CANDIDATE 아니면 409 COMPLETION_NOT_ELIGIBLE(리뷰 S2 순서); status=COMPLETED, approvedAt=now, 스냅샷 유지.
- 추적: W3/W4, BR-U5-4/5, 정수 판정.

### Step 8: SessionCompletionGate 실제 구현 배선 (seam 교체)
- [x] `session/service/SessionBackedCompletionGate.java`(@Component implements `com.learnkk.meeting.service.SessionCompletionGate`) — `allScheduledSessionsEnded` = SessionService read 위임.
- [x] 기존 `meeting/service/NoSessionsCompletionGate.java` **삭제**(중복 @Component 빈 충돌 방지; 스텁은 Bolt 6에서 교체가 설계 의도).
- 추적: ADR-007 R-2, MeetingApprovalService.completeMeeting 게이팅(무회귀: S=0 vacuous-true).

### Step 9: DTO + Controller + 인터셉터
- [x] DTO(record, camelCase, `from()` 팩토리): `SessionResponse`(id,meetingId,week,scheduledAt,checkInWindowMinutes), `AttendanceResponse`(sessionId,menteeId,checkedInAt), `AttendanceSummaryResponse`(attended,totalScheduled,rate), `MenteeCompletionResponse`(meetingId,menteeId,status,attendedCount,totalScheduled,approvedAt). Request: `CreateSessionRequest`,`UpdateSessionRequest`(@Valid).
- [x] `session/web/SessionController.java`: `POST /api/meetings/{id}/sessions`(addSession,201), `PUT /api/sessions/{id}`(updateSession,200), `GET /api/meetings/{id}/sessions`(listSessions,200).
- [x] `session/web/AttendanceController.java`: `POST /api/sessions/{id}/attendance`(checkIn,201), `GET /api/meetings/{id}/my-attendance`(getMyAttendance,200).
- [x] `session/web/CompletionController.java`: `POST /api/meetings/{id}/completions/compute`(computeCompletion,200, 소유멘토/admin), `GET /api/meetings/{id}/completions`(getCompletions,200), `POST /api/admin/meetings/{id}/completions/{menteeId}/approve`(approveMenteeCompletion,200, admin ④).
- [x] `SessionAuthInterceptor` 확장: 신규 Pattern 상수(`^/api/meetings/\d+/sessions$`, `^/api/sessions/\d+$`, `^/api/sessions/\d+/attendance$`, `^/api/meetings/\d+/my-attendance$`, `^/api/meetings/\d+/completions$`, `^/api/meetings/\d+/completions/compute$`) + method 분기 전부 protected. `/api/admin/**`는 기존 prefix로 이미 보호.
- 추적: component-methods, 계약 #1, SessionAuthInterceptor 관례.

### Step 10: 백엔드 테스트 (Standard)
- [x] `SessionServiceTest`(Mockito): addSession 소유멘토/비소유403/비IN_PROGRESS409, updateSession, listSessions, allScheduledSessionsEnded(세션 종료/미종료/빈 세션 경계).
- [x] `AttendanceServiceTest`(Mockito): checkIn 정상, 창밖(이르/늦)→409 WINDOW_CLOSED, 비참여자→403, 비IN_PROGRESS→409, **멱등(중복 check-in 1건 유지)**; getMyAttendance a/S·**S=0→rate 0**.
- [x] `CompletionServiceTest`(Mockito): **80% 경계(a*100==80*S 수료후보, 미만 미수료)**, **S=0 후보 보류**, computeCompletion 참여자별 판정; approve 정상, 미충족→409 NOT_ELIGIBLE, **이미 COMPLETED→409 ALREADY_APPROVED(순서)**, 비admin→403.
- [x] `SessionControllerTest`/`AttendanceControllerTest`/`CompletionControllerTest`(@WebMvcTest, @MockBean): 라우트 상태코드·인가·검증.
- [x] `integration/SessionAttendanceIntegrationTest`(AbstractIntegrationTest 상속): 세션 생성→시간창 내 checkIn→출석율→computeCompletion→④ approve end-to-end; 창밖 409; 멱등.
- [x] 기존 `MeetingApprovalService`/completeMeeting 게이팅 회귀 확인(세션 미종료 시 409 MEETING_SESSIONS_NOT_ENDED — 신규 gate).
- 추적: team.md Testing Posture, BR-U5-2/4(수학 필수), 80% floor.

### Step 11: 계약 #1 — openapi.yaml
- [x] version bump(`0.3.0-bolt3`→`0.4.0-bolt6`), tags `sessions`·`attendance`·`completions` 추가, description 갱신.
- [x] paths: 위 8개 라우트. schemas: SessionResponse, AttendanceResponse, AttendanceSummaryResponse, MenteeCompletionResponse, CreateSessionRequest, UpdateSessionRequest. 409(WINDOW_CLOSED/NOT_ELIGIBLE/ALREADY_APPROVED/MEETING_NOT_ACTIVE)·403 응답.
- [x] `OpenApiContractTest`에 신규 응답 DTO 정합 케이스(`xxx_conformsToSchema`) 추가.

### Step 12: Frontend API 계층
- [x] `api/sessions.ts`(auth `session.ts`와 충돌 회피 위해 파일명 `sessions.ts`): `listSessions(meetingId)`, `addSession(meetingId, body)`, `updateSession(sessionId, body)`, `checkIn(sessionId)`, `getMyAttendance(meetingId)`, `computeCompletions(meetingId)`, `listCompletions(meetingId)`, `approveCompletion(meetingId, menteeId)`.
- [x] `api/types.ts`: SessionResponse·AttendanceResponse·AttendanceSummaryResponse·MenteeCompletionResponse·CompletionStatus 타입. `api/errors.ts`: SESSION_*/ATTENDANCE_*/COMPLETION_* 한국어 메시지. `api/index.ts` re-export(sessionsApi).

### Step 13: Frontend 멘토 세션 관리 (MentorHub)
- [x] MentorHub 각 모임 카드(status=IN_PROGRESS)에 세션 관리 진입: 주차별 세션 추가(week·scheduledAt·window), 세션 목록·시간 변경 UI. 변경 시 멘티 현황 반영 안내. `data-testid`.
- 추적: FE 화면(멘토 세션 관리), W1.

### Step 14: Frontend 멘티 출석 팝업 + 현황/수료 (MenteeLearning)
- [x] `mentee-session-note` placeholder 제거 → 신청 모임별 세션 목록. 예정 시각 도래 시 클라이언트 타이머 팝업 → 출석 체크 버튼(창 밖이면 서버 409 한국어 안내). 출석율(a/S)·수료 상태(후보/확정) 표시.
- 추적: W2/W5, BR-U5-2, ADR-005(클라이언트 타이머), CC-2/CC-3.

### Step 15: Frontend 관리자 ④ 수료 확정 (AdminApprovalPage)
- [x] AdminApprovalPage(또는 모임 상세)에 IN_PROGRESS/COMPLETED 모임의 수료후보 목록: computeCompletions 트리거 → 후보 목록 → 각 멘티 ④ 확정 버튼(approveCompletion). 이미 확정/미충족 409 한국어 안내.
- 추적: W4, BR-U5-5, ④ 관리자.

### Step 16: Frontend 테스트
- [x] MentorHub 세션 관리(추가·변경), MenteeLearning 출석 팝업·창밖 409·출석율/수료 표시, AdminApprovalPage ④ 확정, sessions api 단위. co-located `*.test.tsx`, 80% 유지.

### Step 17: 문서
- [x] README에 Bolt 6 범위(세션·출석·수료 판정·④) + 엔드포인트 반영.

---

## Assumptions

- checkInWindowMinutes 기본 120분, 세션 활성 시점=모임 IN_PROGRESS, ④/③ 순서 무관(파일럿), S=0 후보 보류, getMyAttendance rate S=0→0.
- 시간창 판정은 요청 시점 `now` 비교(스케줄러리스 ADR-005) — 서버 배치 없음, 팝업은 FE 타이머.
- U5→U3/U4 read는 서비스 포트 경유(getMeeting / listActiveMenteeIds·isActiveParticipant). meeting/enrollment 테이블 직접 접근 금지.
- NoSessionsCompletionGate 삭제 후 SessionBackedCompletionGate로 교체(중복 빈 회피). S=0 vacuous-true로 완료 게이팅 무회귀.
- ci-pipeline·operation은 project.md Scope Override로 build-and-test(3.6) 이후 SKIP.

## 테스트 전략 (Standard)

- 컴포넌트당 5~8 단위 + 관통 통합. 시간창(창밖 409)·멱등(중복 check-in)·80% 경계(`a*100==80*S`)·S=0 경계는 필수 커버. 이 환경 Testcontainers 미가용 시 라이브 API E2E로 보완.
