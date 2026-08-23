# Code Generation Plan — Bolt 2 Meeting 완성 (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 2 = U3 Meeting 잔여(상태머신 전 전이 T3~T6·문항 게이팅·멘토 운영 허브). Brownfield: Bolt 1 코드(260817-bolt1-skeleton 결과) in-place 확장. 상속 설계: 260731-learnkk-crew intent의 U3 functional-design(business-logic-model/business-rules/domain-entities)·nfr-requirements·application-design(ADR-006 상태머신 소유·ADR-007 read 교차참조). 규칙: team.md(monorepo·3계층·계약우선·test-alongside·80% floor)·project.md(스택 lock·camelCase/snake_case·전역 에러 스키마)·construction.md. -->

## 목표 (Definition of Done — bolt-plan Bolt 2)

Bolt 1 위에 모임 상태머신을 **완결**한다:

> 개설(PENDING_APPROVAL) →①승인(RECRUITING) →**모집확정**(READY_TO_START | CANCELLED) →**②시작**(IN_PROGRESS) →**③완료**(COMPLETED) · 반려/취소

- 전이표 BR-U3-1의 T3/T4/T5/T6를 구현(T1/T2는 Bolt 1 완료). 불법 전이는 항상 409 `MEETING_INVALID_TRANSITION`.
- 문항 빌더 게이팅(BR-U3-7, ②시작 이후 편집 금지) 검증·테스트.
- 멘토 운영 허브: `listMyMeetings`(U3 소유) 백엔드 + FE 목록/상태/다음 액션.
- test-alongside, BE/FE 각 80% line coverage floor + 전이 분기(정상·불법 전이 409·경합) 시나리오 커버.
- 확신 가설(bolt-plan): 상태머신·승인 4지점(③=관리자 직접)이 불법 전이 방지(409)와 함께 정확히 동작.

## 상속·현황 (Bolt 1 기반)

- **완료(Bolt 1):** Meeting/SurveyQuestion 엔티티·리포지토리, `MeetingService`(createMeeting/getMeeting/listRecruiting), `SurveyTemplateService`(upsert/get + isLocked), `MeetingApprovalService`(T1 approveCreation/T2 rejectCreation), `MeetingRepository.transitionStatus`(조건부 UPDATE 프리미티브), 전역 에러/enum/Principal, FE meetings feature(목록·개설·문항빌더·관리자 lookup·멘토 lookup 허브).
- **MeetingStatus enum:** 7개 값 전부 선언됨 — 신규 enum 값 불필요.
- **재사용 프리미티브:** `transitionStatus(id, from, to, rejectReason)` → 0 rows면 409. T3~T6 전부 이 위에 구현.

## 범위 밖 (forward-dependency, 이월 — placeholder/seam)

- **T6 세션 종료 read(U5, Bolt 6):** `SessionCompletionGate` 시임 뒤로. Bolt 2 구현체는 세션 모듈 부재 → 통과. 상태 write(COMPLETED)는 U3 단일 소유로 지금 구현.
- **멘토 허브 신청자(U4, Bolt 3)·사전설문 응답(U8, Bolt 7) 조합:** FE placeholder 유지, listMyMeetings 기반 자기 모임/상태/액션까지만.
- **관리자 승인 큐 목록(U9, Bolt 8):** 큐 조회=U9. Bolt 2 관리자 FE는 lookup-by-id + 상태 인지 액션 버튼으로 확장.

---

## 실행 단계 (layer-by-layer, 의존 → 의존자 순)

### Step 0: 상속 순환 의존 해소 (Bolt 1 리뷰어 follow-up)
- [x] `kernel/config/WebConfig.java`가 `auth.web.SessionAuthInterceptor`를 직접 import → C0 leaf(kernel) 불변식(components.md·ADR-007) 위반 해소: WebConfig를 앱 레벨 `com.learnkk.config`로 이동(또는 인터셉터를 포트 인터페이스로 주입). 기존 동작·테스트 보존.
- 추적: Bolt 1 reviewer follow-up, ADR-007 C0 leaf 불변식

### Step 1: 도메인 계약 — 세션 종료 게이트 시임 (T6 forward-dep)
- [x] `meeting/service/SessionCompletionGate.java`(인터페이스): `boolean allScheduledSessionsEnded(Long meetingId)`.
- [x] Bolt 2 기본 구현 `NoSessionsCompletionGate`(또는 `@Primary` 스텁): U5 세션 모듈 부재 → `true` 반환. Javadoc으로 "Bolt 6(U5)가 실제 SessionService read로 교체" 명시.
- 추적: BR-U3-5·business-logic-model W2 `completeMeeting`, ADR-007 R-2, memory.md forward-dep tradeoff

### Step 2: 상태 전이 T3~T6 — `MeetingApprovalService` in-place 확장 (C2)
- [x] `MeetingApprovalService.java:60` 플레이스홀더 대체. 공통 전처리: requireAdmin(403)·ensureExists(404)·transitionStatus(0 rows→409 MEETING_INVALID_TRANSITION) 재사용.
- [x] `confirmRecruitment(admin, meetingId, proceed, reason)` — **T3/T4**: RECRUITING → proceed?READY_TO_START:CANCELLED. proceed=false 시 reason 저장(reject_reason 재사용). (신청 수는 FE 표시 보조; 분기는 proceed 입력만 — U4 백엔드 read 없음, business-logic-model 준수.)
- [x] `approveStart(admin, meetingId)` — **T5**: READY_TO_START → IN_PROGRESS.
- [x] `completeMeeting(admin, meetingId)` — **T6**: IN_PROGRESS 확인 → `SessionCompletionGate.allScheduledSessionsEnded` false면 409 `MEETING_SESSIONS_NOT_ENDED` → transitionStatus IN_PROGRESS→COMPLETED.
- [x] 클래스 Javadoc(15-18) 갱신: Bolt 2가 T3~T6 완결.
- 추적: BR-U3-1 T3~T6, business-logic-model W2, US-3.4/6.1/7.3

### Step 3: 신규 에러 코드 + 반려/취소 사유 필수화
- [x] `kernel/error/ErrorCodes.java`에 `MEETING_SESSIONS_NOT_ENDED` 추가(Meeting 도메인).
- [x] `meeting/dto/RejectRequest.java` reason `@NotBlank` 필수화(설계 [assumption] 반려 사유 필수). Javadoc의 "optional in Bolt 1" 갱신.
- 추적: BR-U3-5(409 세션 미종료), business-rules Assumptions(반려 사유 필수)

### Step 4: 컨트롤러 라우트 — `MeetingApprovalController` in-place 확장
- [x] `POST /api/admin/meetings/{id}/confirm-recruitment` (body: `{proceed:boolean, reason?:string}`) → T3/T4.
- [x] `POST /api/admin/meetings/{id}/approve-start` → T5.
- [x] `POST /api/admin/meetings/{id}/complete` → T6.
- [x] 신규 DTO `ConfirmRecruitmentRequest{proceed, reason}`(record, `@NotNull proceed`). 응답은 기존 `MeetingResponse`.
- 추적: W2, 계약 #1

### Step 5: 멘토 운영 허브 백엔드 — `listMyMeetings` (US-2.3)
- [x] `MeetingRepository`에 `Page<Meeting> findByMentorId(Long mentorId, Pageable)` 추가.
- [x] `MeetingService.listMyMeetings(Principal, Pageable)` — role=MENTOR 확인(403), 자기 모임만 → `PageResponse<MeetingSummary>`(또는 상태 포함 뷰).
- [x] `MeetingController`: `GET /api/meetings/mine`(인증·멘토) 라우트 추가. 기존 `list`의 status=recruiting 외 거부(65행)는 유지(전체 status 목록은 U9/Bolt 8) — 멘토 자기목록은 전용 라우트로 분리.
- 추적: BR-U3-6, business-logic-model W4(백엔드는 U3 소유만), US-2.3

### Step 6: 백엔드 테스트 (test-alongside, Standard)
- [x] `MeetingApprovalServiceTest` 확장: T3 정상/미달-proceed=false 취소/T4·비RECRUITING 409, T5 정상/비READY 409, T6 정상/세션미종료 409(gate mock false)/비IN_PROGRESS 409, 비관리자 403, 이중 전이 경합 409.
- [x] `SessionCompletionGate` 스텁 단위 테스트(통과 반환).
- [x] `SurveyTemplateServiceTest`: ②시작(IN_PROGRESS) 이후 문항 편집 409 게이팅 재확인(BR-U3-7).
- [x] `MeetingServiceTest`: listMyMeetings 자기모임 필터·비멘토 403.
- [x] `MeetingControllerTest`/`MeetingApprovalControllerTest`: 신규 라우트 상태코드·인가(@WebMvcTest).
- [x] `integration/MeetingIntegrationTest` 확장: 개설→①→모집확정→②→③ end-to-end 전이 통과 + 불법 전이 409.
- 추적: team.md Testing Posture(80% floor·분기 시나리오), construction.md(happy+edge)

### Step 7: API 계약 #1 — `contracts/openapi.yaml` 확장
- [x] version bump(`0.1.0-bolt1` → `0.2.0-bolt2`).
- [x] 신규 paths: `/api/admin/meetings/{id}/confirm-recruitment`·`/approve-start`·`/complete`, `/api/meetings/mine`.
- [x] 신규 schema `ConfirmRecruitmentRequest`. `RejectRequest.reason` required 반영. admin 액션 409(MEETING_SESSIONS_NOT_ENDED 포함) 응답 명시.
- [x] 계약 테스트(OpenApiContractTest) 신규 응답 DTO 정합 확장.
- 추적: team.md 계약 #1·계약 테스트 계층

### Step 8: Frontend API 계층 — `api/admin.ts`·`api/meetings.ts`
- [x] `adminApi`: `confirmRecruitment(id, proceed, reason?)`, `approveStart(id)`, `complete(id)`.
- [x] `meetingsApi`: `listMine(params)` (GET /api/meetings/mine).
- [x] `api/types.ts`: 필요한 요청/응답 타입 추가.
- 추적: 단일 API client(ErrorPayload→한국어), components.md FE

### Step 9: Frontend 멘토 운영 허브 — `MyLearningPage.tsx`(MentorHub) 확장 (US-2.3)
- [x] lookup-by-id placeholder 제거 → `listMine` 기반 자기 모임 목록(상태 뱃지·다음 액션 안내). 신청자/사전설문 응답 조합은 placeholder 주석 유지(U4 Bolt 3·U8 Bolt 7 이월).
- [x] `data-testid` 부여. 멘티 branch placeholder는 신청(U4) 이월로 유지.
- 추적: business-logic-model FE 멘토 운영 허브, meetingStatus.ts(7상태 라벨 기존 지원)

### Step 10: Frontend 관리자 액션 — `AdminApprovalPage.tsx` 확장
- [x] lookup-by-id 유지하되 조회된 모임 status에 따라 상태 인지 액션 버튼 노출: RECRUITING→모집확정(진행/취소+사유), READY_TO_START→②시작, IN_PROGRESS→③완료. 종료 상태→액션 없음.
- [x] 409(MEETING_INVALID_TRANSITION·MEETING_SESSIONS_NOT_ENDED) 한국어 메시지 매핑. 반려/취소 사유 필수 입력. `data-testid` 부여. 승인 큐 목록은 Bolt 8(U9) placeholder 주석 유지.
- 추적: business-logic-model 관리자 액션(큐=U9, 액션=U3)

### Step 11: Frontend 테스트 (RTL + Vitest)
- [x] MentorHub: listMine 렌더·상태 뱃지·빈/에러 상태.
- [x] AdminApprovalPage: status별 액션 버튼 표시·모집확정 다이얼로그·②/③ 호출·409 매핑(API client mock).
- [x] admin/meetings api 단위(신규 호출 경로).
- 추적: team.md Testing Posture(FE 80% floor), CC-3(로딩/빈/에러)

### Step 12: 문서·정리
- [x] 루트 `README.md`에 Bolt 2 범위(상태머신 완결) 반영, Bolt 3+ 이월 명시.
- [x] 인라인 문서(전이 가드·SessionCompletionGate 시임). OpenAPI가 API 문서.
- 추적: 인수인계

---

## Assumptions

- 상태머신 전이는 조건부 UPDATE(`transitionStatus`) 재사용으로 직렬화(낙관적 가드) — Bolt 1 프리미티브 유지.
- T6 세션 종료 전제는 `SessionCompletionGate` 시임으로 분리, Bolt 2 스텁은 통과(세션 모듈 부재) — Bolt 6가 U5 read 주입.
- 모집확정 분기는 관리자 `proceed` 입력만 사용(U4 신청 수는 화면 보조, 백엔드 read 없음 — business-logic-model 준수).
- 반려/취소 사유 필수(설계 [assumption]), 취소 사유는 reject_reason 컬럼 재사용 → **스키마 변경(V4 마이그레이션) 불필요**.
- 문항 게이팅은 기존 isLocked(IN_PROGRESS 이후 잠금)이 BR-U3-7과 정합 → 검증·테스트만, 로직 변경 없음.

## 테스트 전략 (Standard)

- 컴포넌트당 단위 5~8 + 핵심 경계 통합 스텁. 전이 분기(정상/불법 409/경합/인가)를 상태별로 커버. BE/FE 각 80% line coverage floor 유지(팀 관행).
