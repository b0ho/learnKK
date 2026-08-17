# Business Logic Model — U5 Session/Attendance (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U5 Session/Attendance(service, L). 스토리 US-6.2/6.3/7.1/7.2/7.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U5), requirements.md(FR6.1~6.3·FR7.1), components.md(C4·sessions feature), component-methods.md(SessionService addSession/updateSession/listSessions·AttendanceService checkIn/getMyAttendance·CompletionService computeCompletion/approveMenteeCompletion), services.md(세션→출석→수료·③ read U5), U1(CompletionStatus/ErrorPayload/Principal). 세션·시간창 출석·수료 판정 수학·FE 화면 정의. -->

## 개요

U5의 세션 일정, 스케줄러리스 시간창 출석, 출석율·80% 수료 판정, ④ 확정, FE 화면을 정의한다. 수료 판정은 정수 연산(a*100≥80*S). U1 CC-1·CompletionStatus 상속.

## 워크플로우

### W1. 세션 일정 (US-6.2 / addSession, updateSession, listSessions)

```
addSession(mentorId, meetingId, {week, datetime}):
  소유 멘토(U3 read) + IN_PROGRESS 확인 아니면 403/409 → Session insert(scheduledAt=datetime)
updateSession(mentorId, sessionId, {datetime}):
  소유 멘토 → scheduledAt 갱신(출석 기록 있으면 경고/제한 [assumption]) → 멘티 현황 반영
listSessions(meetingId): 세션 목록(주차·scheduledAt) → 멘티/멘토/U4 현황·U9 read-out
```

### W2. 팝업 출석 (US-6.3 / checkIn) — 스케줄러리스 시간창

```
checkIn(menteeId, sessionId):
  1. 참여자(U4 APPLIED read)? 아니면 403
  2. 모임 IN_PROGRESS? 아니면 409
  3. now ∈ [scheduledAt, scheduledAt + checkInWindowMinutes]? 아니면 409 ATTENDANCE_WINDOW_CLOSED
  4. Attendance upsert(unique(session,mentee) — 멱등)
  5. return AttendanceResponse
getMyAttendance(menteeId, meetingId):
  a = 출석 세션 수, S = 전체 예정 세션 수
  rate = (S > 0) ? a/S : 0    # S=0(세션 미등록)이면 0나눗셈 회피 → rate=0(현황상 'N/A')
  → {attended:a, totalScheduled:S, rate}
```

- **스케줄러리스(ADR-005):** 창 판정은 checkIn 요청 시점 `now` 비교 — 백그라운드 잡·팝업 트리거 서버 스케줄 없음. FE가 예정 시각에 팝업 노출(클라이언트), 서버는 시간창 검증만.

### W3. 수료 자동 판정 (US-7.1 / computeCompletion)

```
computeCompletion(meetingId):
  participants = U4.participants(meetingId)   # APPLIED read (U5→U4, 정방향 비순환)
  S = count(session WHERE meetingId)
  for each mentee in participants:
     a = count(attendance WHERE session.meetingId AND menteeId)
     if S > 0 and a*100 >= 80*S:   # 정수 판정
         status = COMPLETION_CANDIDATE
     else:
         status = NOT_COMPLETED    # S==0이면 후보 판정 보류(미수료 유지)
     upsert mentee_completion(meetingId, menteeId, status, a, S)
  return MenteeCompletion[]
```

### W4. ④ 관리자 수료 확정 (US-7.2 / approveMenteeCompletion)

```
approveMenteeCompletion(admin, meetingId, menteeId):
  role==ADMIN? 아니면 403
  mc = mentee_completion 조회
  mc.status == COMPLETED → 409 COMPLETION_ALREADY_APPROVED (중복 — 먼저 검사)
  mc.status != COMPLETION_CANDIDATE → 409 COMPLETION_NOT_ELIGIBLE (미충족)
  → status=COMPLETED, approvedAt=now (스냅샷 a/S 유지)
```

### W5. 멘티 현황·수료 결과 (US-7.4)

```
getMyAttendance/수료상태: 본인 출석율·수료 상태(후보/확정) 조회
```

- 멘티 현황 화면은 U4 신청 + U5 세션/출석/수료를 FE 조합(U4 W4 참조).

## read-out (U5가 타 Unit에 제공)

- `allScheduledSessionsEnded(meetingId)`: 모든 session의 `scheduledAt + checkInWindowMinutes < now`? → U3 ③완료 전제(ADR-007 R-2, U3→U5 read).
- `listSessions(meetingId)`: U4 멘티 현황(FE)·U9 모니터링 read.

## FE 화면 (sessions feature)

- **멘토 세션 관리:** 주차별 세션 추가·시간 변경 UI(복수 세션). 변경 시 멘티 현황 반영 안내.
- **멘티 출석 팝업:** 예정 시각 도래 시 팝업(클라이언트 타이머) → 출석 체크 버튼. 창 밖이면 서버 409 안내.
- **멘티 현황/수료:** 출석율(a/S)·수료 상태(후보/확정) 표시.
- **관리자 ④:** 승인 큐(U9)에서 수료후보 확정 액션 → 이 Service.
- 접근성(CC-2)·목록 상태(CC-3) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3(모임 상태·소유 멘토), U4(참여자=수료 대상). U5 depends_on U3/U4(DAG) — 정방향 비순환.
- **read-out:** U3(③ 세션 종료), U4/U9(세션 일정·현황). U5 데이터를 상위가 read.
- **write:** session/attendance/mentee_completion(U5 소유).

## 에러·엣지 케이스

- 시간창 밖 출석 → 409 ATTENDANCE_WINDOW_CLOSED.
- 비참여자 출석 → 403. 시작 전(②전) 출석 → 409.
- 중복 check-in → 멱등(무해).
- 미충족 멘티 ④ 확정 → 409 COMPLETION_NOT_ELIGIBLE.
- S=0(세션 미등록) → 후보 판정 보류(미수료).
- 세션 추가로 S 증가 → 출석율 하락, 재판정 반영.

## Assumptions & Open Questions

- **[decided]** a*100≥80*S 정수 판정, 분모=전체 예정 세션, mentee_completion U5 소유.
- **[assumption]** 시간창 120분, 세션 활성 시점(IN_PROGRESS), S=0 보류, ④/③ 순서 무관.
- **[open]** 세션 변경 통지(A6), 과거 세션 편집 제약, U3/U4 read 포트 시그니처 정합.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U5 Session/Attendance, kind=service, L; scope: business-logic-model.md + business-rules.md + domain-entities.md against the consumed inception contracts and the U1 shared kernel)

Verdict: READY

I walked in trying to break three things: the 80% completion math (denominator, integer test, S=0), the scheduler-less window judgment (ADR-005), and the cross-unit read graph (U3↔U5 / U4↔U5 as a hidden write cycle). None survived into a blocking finding. The core is sound: the certification math is correct and division-free, the window judgment is request-time only with no background job, and every cross-unit edge resolves to a **read** with writes single-owned — no write cycle. The findings below are quality/edge-case items a developer can close without architectural escalation.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Completion math — PASS.** W3/BR-U5-4 use `if S > 0 and a*100 >= 80*S` → `COMPLETION_CANDIDATE`, else `NOT_COMPLETED`. Denominator S = `count(session WHERE meetingId)` = 전체 예정 세션 (FR6.3 rev-us "분모=전체 예정 세션"), matching unit-of-work.md U5 note `a*100≥80*S`. The test is pure integer multiplication — **no division, no float** — so no div-by-zero in the judgment path. The `S > 0` guard is the correct fix for the S=0 trap that BR-U5-4 itself names ("식은 `a*100 >= 0` 항상 참 → S=0이면 후보 판정 보류"): without the guard, `80*S = 0` would mark every mentee a candidate; with it, S=0 falls to `NOT_COMPLETED` (candidate withheld). S recomputes on session add/remove (BR-U5-3 "판정 시점 S 기준"; edge case "세션 추가로 S 증가 → 재판정"). ④ snapshot preserved (`attendedCount/totalScheduled` on mentee_completion; W4 "스냅샷 a/S 유지").
- **CompletionStatus transitions — PASS.** Uses the U1 enum verbatim (NOT_COMPLETED / COMPLETION_CANDIDATE / COMPLETED — confirmed against U1 domain-entities.md CompletionStatus; judgment logic correctly deferred to U5 per U1). ④ approve requires COMPLETION_CANDIDATE else 409 `COMPLETION_NOT_ELIGIBLE`; COMPLETED is terminal (재전이 불가). Role gate `role==ADMIN else 403` matches U1 BR-U1-5.
- **Scheduler-less window (ADR-005) — PASS.** checkIn valid only when `now ∈ [scheduledAt, scheduledAt + checkInWindowMinutes]` (W2 step 3, BR-U5-2, domain-entities). No background job / no server-side popup trigger (W2 note; services.md "배치/스케줄러 없음 — 시간 판정은 요청 시점"). FE popup is client-side; server validates the window only. Idempotent via `unique(sessionId, menteeId)` (W2 step 4, Attendance entity). Gated to IN_PROGRESS (W2 step 2) and participant-only (W2 step 1, U4 read).
- **Cross-unit reads — PASS, no write cycle.** U5 `depends_on [U1,U2,U3,U4]` (unit-of-work summary table). U5→U3 (meeting status/owner) and U5→U4 (participants) are forward/declared reads. Read-outs: `allScheduledSessionsEnded → U3 ③` (U3→U5 read) and `listSessions → U4/U9`. The **U3↔U5** pair is read/read: U5→U3 declared dep + U3→U5 ③-precondition back-edge, both reads, resolved via ADR-007 R-2 (confirmed against component-methods `completeMeeting` note "전제조건은 C4 read로 확인, 쓰기는 C2 단일 소유" and the components.md dependency review's acyclic write-graph finding). The **U4↔U5** pair is handled asymmetrically and correctly: the backend edge is unidirectional U5→U4 (computeCompletion reads participants, C4→C3), while the reverse (멘티 현황 needs U5 sessions) is done as **FE composition** (W5 "U4 신청 + U5 세션/출석/수료를 FE 조합"; story-map US-3.5 read), so no backend C3→C4 compile edge and no cycle. Writes are disjoint (U5 owns session/attendance/mentee_completion; U3 owns completion transition; U4 owns enrollment).
- **④ vs ③ independence — PASS.** W4/BR-U5-5 make mentee ④ (U5) independent of meeting ③ (U3), flagged `[assumption]` (파일럿: ③ 전후 무관). Consistent with FR7.1/FR7.2 as distinct approval points.
- **Method fidelity to component-methods C4 — PASS.** addSession(mentorId, meetingId, {week, datetime}), updateSession(mentorId, sessionId, {datetime}), listSessions(meetingId), checkIn(menteeId, sessionId), getMyAttendance(menteeId, meetingId)→{attended,totalScheduled,rate}, computeCompletion(meetingId)→MenteeCompletion[], approveMenteeCompletion(admin, meetingId, menteeId) — all match signatures and return shapes.
- **Story coverage — PASS.** US-6.2 (W1), US-6.3 (W2), US-7.1 (W3), US-7.2 (W4), US-7.4 (W5) all covered; matches story-map U5 assignments.
- **Epistemic status — PASS, no silent promotion.** [decided]: 정수 판정, 분모=전체 예정 세션, mentee_completion U5 소유. [assumption]/[open]: 시간창 120분, 활성 시점(IN_PROGRESS), S=0 처리, ③/④ 순서, 과거 세션 편집, U3/U4 read 포트 시그니처. Consistent across all three files.
- **Sensors — PASS (one gap, see S4).** required-sections: business-logic-model 7 H2, business-rules 11 H2, domain-entities 4 H2 (all ≥2). upstream-coverage: business-logic-model.md and domain-entities.md reference all six consumed artefacts (unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services).

### Suggestions (non-blocking)

- **S1 — `getMyAttendance` returns `rate: a/S`, which IS a real division and has no S=0 guard (div-by-zero / NaN).** W2 defines `getMyAttendance → {attended:a, totalScheduled:S, rate:a/S}`. Unlike computeCompletion (which guards `S > 0`), this display path divides by S with no guard. S=0 is a realistic state — right after ② start, before the mentor schedules any session — so a mentee opening the 현황 screen would hit 0/0. Recommend the design state explicitly that `rate = 0` (or "N/A") when S=0, mirroring the computeCompletion guard. Evidence-grounded (literal `a/S`), trivially closeable, does not affect the certification math — hence not blocking.
- **S2 — `approveMenteeCompletion` guard ordering makes the COMPLETED branch dead code.** W4 evaluates `mc.status != COMPLETION_CANDIDATE → 409 COMPLETION_NOT_ELIGIBLE` *before* `mc.status == COMPLETED → 409 (중복)`. Since COMPLETED already satisfies `!= COMPLETION_CANDIDATE`, an already-certified mentee returns `COMPLETION_NOT_ELIGIBLE` rather than a duplicate code. Both are HTTP 409, so no status divergence, but the error `code` is misleading. Recommend testing the terminal/duplicate case first, then the eligibility case (BR-U5-5 already distinguishes the two intents).
- **S3 — U4 participant read-port signature is unresolved and the only contract method has the wrong auth context.** W3/domain-entities call `U4.participants(meetingId)` for computeCompletion, but component-methods EnrollmentService exposes only `listApplicants(mentorId, meetingId)` (mentor-auth) and `listMyEnrollments(menteeId)` (mentee-self). computeCompletion runs in a system/admin context, so neither fits, and `participants(meetingId)` is not a declared method. The **edge is legal** (forward per DAG U5→U4; component-dependency documents C4→C3 read) and the design honestly flags "U3/U4 read 포트 시그니처 정합" as `[open]`, so this is a contract-sync item rather than a U5 logic defect. Recommend U4 expose a neutral participant-read port (or widen listApplicants' auth) and pin its signature so a developer needn't guess.
- **S4 — business-rules.md omits the `services.md` upstream reference (upstream-coverage sensor risk).** business-logic-model.md and domain-entities.md both cite services.md in their header source-comment; business-rules.md does not (it cites unit-of-work, story-map, requirements, components, component-methods, U1). The 세션→출석→수료 orchestration from services.md IS reflected in the rules, so this is a citation-formality gap, not a content gap — add a services.md reference to keep the sensor green across all three outputs.
- **S5 — ATTENDANCE_WINDOW_CLOSED status code is written "400/409" in BR-U5-2 but 409 everywhere else.** business-logic-model W2, the 에러·엣지 케이스 block, and the intent all use 409; BR-U5-2 waffles "400/409". Per U1 CC-1 a closed window is a timing/state conflict (409), not input validation (400). Recommend fixing BR-U5-2 to 409 for consistency.

Verdict: READY
