# Business Logic Model — U4 Enrollment (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U4 Enrollment(service). 스토리 US-3.2/3.3/3.5(unit-of-work-story-map.md). 출처: unit-of-work.md(U4), requirements.md(FR3.2/3.3/3.5), components.md(C3·enrollment feature), component-methods.md(EnrollmentService apply/cancel/listApplicants/listMyEnrollments), services.md(정원·상태 U3 read·현황 U5 read), U1(ErrorPayload/Principal/Pagination). 선착순 정원 알고리즘·취소·현황·FE 화면 정의. -->

## 개요

U4의 신청 워크플로우(선착순 정원·중복·취소)와 멘티 현황(FE 조합), FE 화면을 정의한다. 정원·모임 상태는 U3 read(ADR-007 R-1).

## 워크플로우

### W1. 신청 (US-3.2 / apply) — 선착순 정원

```
apply(menteeId, meetingId):
  1. Principal.role==MENTEE? 아니면 403
  2. meeting = U3.read(meetingId)  # ADR-007 R-1 (U4→U3, 순환 아님)
     없음 → 404; status != RECRUITING → 409 ENROLLMENT_NOT_OPEN
  3. [원자 구간] 모임 단위 직렬화(BR-U4-1: pg_advisory_xact_lock(meetingId)
     또는 SERIALIZABLE; U3 meeting 행은 잠그지 않음 — 모듈 소유):
       active = count(enrollment WHERE meeting_id=? AND status=APPLIED)
       active >= meeting.capacity → 409 ENROLLMENT_FULL
       insert enrollment(APPLIED, appliedAt=now)   # unique(meeting,mentee) 위반 → 409 ENROLLMENT_DUPLICATE
  4. return EnrollmentResponse
```

- 잔여 1석 동시 신청: 3의 직렬화로 한 건만 성공, 나머지 409 ENROLLMENT_FULL. 중복 신청: unique로 차단.

### W2. 취소 (US-3.3 / cancel)

```
cancel(menteeId, meetingId):
  1. 본인 신청 조회. 없음 → 404
  2. meeting.status ∈ {RECRUITING, READY_TO_START}? 아니면(②후) 409 ENROLLMENT_CANCEL_FORBIDDEN
  3. status=CANCELLED, cancelledAt=now  # 정원에서 제외 → 빈자리 복귀
  4. return
```

### W3. 신청자 목록 (US-2.3 read / listApplicants)

```
listApplicants(mentorId, meetingId):
  소유 멘토/관리자 확인(403) → APPLIED 신청자 목록(멘티 정보 U2 read 최소)
```

- U3 운영 허브 화면이 이 엔드포인트를 호출(U3→U4, ADR-007 R-1).

### W4. 멘티 현황 (US-3.5) — FE 화면 조합

```
# 백엔드 (U4 소유만)
listMyEnrollments(menteeId):
  본인 신청 목록(모임·상태·appliedAt) → EnrollmentStatusResponse[]

# FE 멘티 현황 화면 (조합 — 백엔드 아님)
myLearningScreen(menteeId):
  enrollments = U4.listMyEnrollments(menteeId)      # 소유
  sessions    = U5.listSessions(meetingId)          # U5 엔드포인트 호출(백엔드 U4→U5 아님, 순환 회피)
  meetingInfo = U3.getMeeting(meetingId)            # 모임 상태
  → 화면 조합(내 신청/모임 + 다음 세션 일정)
```

- U5 세션 일정 read는 **FE 화면 조합 전용**(U5 depends_on에 U4 존재 → 백엔드 U4→U5 금지).

## FE 화면 (enrollment feature)

- **신청 화면:** 모임 상세에서 신청 버튼(RECRUITING·미신청 시). 정원 마감/중복 시 서버 409 → 안내 메시지.
- **멘티 현황(내 러닝):** 내 신청 목록(모임·상태·다음 액션) + 다음 세션 일정(U5 조합). 취소 버튼(②전만 노출, 서버 재검증).
- 접근성(CC-2)·목록 상태(CC-3) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3 모임 상태·정원(apply/cancel 판정, ADR-007 R-1, U4→U3 방향).
- **FE 화면 조합:** 멘티 현황의 U5 세션 일정·U3 모임 정보 — 백엔드 U4→U5 없음(순환 회피).
- **read-out(타 Unit이 U4 read):** U3 운영 허브(신청자, U3→U4), U5 수료 대상(참여자, U5→U4), U9 모니터링.
- **write:** enrollment만(U4 소유).

## 에러·엣지 케이스

- 잔여 1석 2인 동시 신청 → 1인 APPLIED, 1인 409 ENROLLMENT_FULL.
- 동일 멘티 2회 신청 → unique 위반 409 ENROLLMENT_DUPLICATE.
- ②시작 후 취소 → 409 ENROLLMENT_CANCEL_FORBIDDEN.
- 취소로 빈자리 → 다른 멘티 신청 성공(정원 재계산).
- 모집 아님(개설신청/시작대기 등) 신청 → 409 ENROLLMENT_NOT_OPEN.
- **status TOCTOU(경미):** status==RECRUITING 검사(step 2)는 원자 구간 밖이고 status는 U3 소유라, 동시 관리자 `confirmRecruitment`(RECRUITING→READY_TO_START)가 검사~insert 사이에 상태를 바꿀 수 있는 좁은 창이 있다. overbooking 불변식은 락으로 유지되며, 창은 관리자 수동 액션·파일럿 규모라 영향 미미. 교차모듈 상태 경합은 U4 로컬 락으로 완전 차단 불가(read-port 모델 내재) — 후속 정합 시 U3 상태 스냅샷 재확인 검토.

## Assumptions & Open Questions

- **[assumption]** 취소 후 재신청 불가, 대기열 없음.
- **[open]** 정원 직렬화 방식(어드바이저리 락 vs SERIALIZABLE) — 구현. 무결성 필수.
- **[open]** U3 정원·상태 read 포트, U5 세션 read 시그니처(U3/U5 functional-design 정합).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U4 Enrollment, kind=service; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts + U1 shared kernel)

Verdict: READY

I walked in assuming the 잔여-1석 race would overbook, that U4 was reaching backward into U5, and that a cross-reference or method would fail to resolve. I could not sustain any of those into a blocking finding. The overbooking invariant is guaranteed by at least two of the three proposed mechanisms, the U4↔U5 cycle is correctly avoided by pushing the session read to FE composition, and every C3 method and cross-unit claim resolves to a consumed contract.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Concurrency / no-overbooking (BR-U4-1) — PASS.** Invariant `active(APPLIED) count ≤ capacity` is soundly guaranteed by two of the three offered mechanisms: (a-advisory) `pg_advisory_xact_lock(meetingId)` serializes every `apply` for a meeting; because the lock is held to transaction end, the next holder's `count(APPLIED)` sees the prior committed insert — count-then-insert is race-free; (b-SERIALIZABLE) count-then-insert under SSI: the phantom insert creates a read-write dependency that Postgres detects and aborts with `serialization_failure`, and the design correctly specifies retry/reject. Capacity is effectively immutable during `RECRUITING` (no requirement edits capacity mid-recruit), and `count` uses `status=APPLIED` only, so a concurrent `cancel` (APPLIED→CANCELLED) can only *lower* the active count — it cannot cause overbooking. `cancel` legitimately does not need the apply lock. The 잔여-1석 case resolves to exactly one `APPLIED` + one `409 ENROLLMENT_FULL`, as claimed.
- **Capacity/status sourced from U3, no write to U3 — PASS.** W1 step 2 reads `meeting` via U3 (`ADR-007 R-1`, U4→U3, legal per DAG `U4 depends_on U3`). "write: enrollment only (U4 소유)" holds; U4 never writes U3's meeting table.
- **Duplicate prevention — PASS.** `unique(meeting_id, mentee_id)` (BR-U4-2, domain-entities) blocks the concurrent double-apply via constraint violation → `409 ENROLLMENT_DUPLICATE`. Consistent with the tagged `[assumption]` "재신청 불가" (a leftover CANCELLED row keeps the pair occupied); the re-design path for a re-apply policy is explicitly noted.
- **Cross-unit direction / cycle avoidance — PASS.** DAG confirms `U5 depends_on [...,U4]`, so a backend `U4→U5` read would be a `U4↔U5` cycle. All three artifacts consistently route the US-3.5 session-schedule read (`U5.listSessions`) through **FE screen composition**, not a backend call — the cycle is genuinely avoided, not merely relabeled. `listApplicants` is U4-owned and consumed by U3's hub screen (`U3→U4` via `ADR-007 R-1` controller/FE composition); since U3's declared deps are `[U1,U2]`, this is correctly kept as composition rather than a backend back-edge. `U4→U2` mentee read (listApplicants) is legal (`U4 depends_on U2`).
- **Cancel rules (BR-U4-3) — PASS (with tagged interpretation).** Cancel allowed in `{RECRUITING, READY_TO_START}` (pre-②), forbidden at `IN_PROGRESS` → `409 ENROLLMENT_CANCEL_FORBIDDEN`, honoring FR3.5's binding "②후 이탈 불가". Freed seat returns to capacity (count excludes CANCELLED). The `READY_TO_START` allowance goes slightly beyond FR3.5's literal "모집 기간 중", but this exact gap ("시작대기 취소, US-3.3") was flagged as an undetermined downstream item in unit-of-work.md and is resolved here as an explicitly-tagged Unit rule — a legitimate, non-silent resolution.
- **Method fidelity to component-methods C3 — PASS.** apply / cancel / listApplicants / listMyEnrollments all present with matching signatures and DTOs (EnrollmentResponse, ApplicantResponse, EnrollmentStatusResponse). U3 (`getMeeting`) and U5 (`listSessions`) calls resolve to real component-methods signatures; the exact U3 capacity read-port shape is correctly left `[open]` rather than over-claimed. Entity non-exposure (NFR8) honored.
- **Story & requirement coverage — PASS.** US-3.2 (W1/BR-U4-1/2), US-3.3 (W2/BR-U4-3), US-3.5 (W4/BR-U4-4) covered; W3 serves US-2.3's applicant read. FR3.2/FR3.3/FR3.5 mapped. CC-1 codes (ENROLLMENT_FULL/DUPLICATE/NOT_OPEN/CANCEL_FORBIDDEN) follow U1's `<DOMAIN>_<REASON>` UPPER_SNAKE convention (BR-U1-1) and map to 409.
- **Epistemic status — PASS.** re-apply-disallowed, no-waitlist (`[assumption]`), locking mechanism and U3/U5 read-port signatures (`[open]`) are tagged consistently across all three files; no silent promotion.
- **Sensors — PASS.** required-sections: business-logic-model 6 H2, business-rules 8 H2, domain-entities 5 H2 (≥2). upstream-coverage: all six consumed artifacts (unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services) referenced. No fenced TS/JS/TSX code, so linter/type-check have nothing to flag (the pseudocode blocks are untagged/plain).

### Suggestions (non-blocking; defer to implementation)

- **S1 — Drop or reframe the `FOR UPDATE` capacity-lock variant.** BR-U4-1(a) offers "모임 정원 read를 `FOR UPDATE`로 획득" as an alternative. Taking `SELECT ... FOR UPDATE` on U3's `meeting` row means U4 directly locking a U3-owned table row, which conflicts with the module-ownership rule (components.md: "타 모듈 데이터는 그 모듈 Service를 통해 접근, 직접 테이블 접근 금지") and is not expressible through a read port. The advisory-lock and SERIALIZABLE options are clean and sufficient, so this is not blocking — but at implementation the FOR UPDATE variant should either be dropped or re-scoped to lock a U4-owned row (e.g. a per-meeting enrollment-counter/aggregate row that U4 owns), not U3's meeting row.
- **S2 — Acknowledge the status-gate TOCTOU.** The `status == RECRUITING` check (W1 step 2) sits *outside* the atomic section, and the meeting's status is owned by U3, so a concurrent admin `confirmRecruitment` (RECRUITING→READY_TO_START) could flip status between the check and the insert, admitting a late applicant. The *overbooking* invariant is unaffected (still lock-protected); the impact is a narrow, low-severity window on a manual admin action at pilot scale. A U4-local lock cannot fully close a cross-module status race, so this is inherent to the read-port model — worth a one-line note in the edge-case list rather than a mechanism change.
- **S3 — Tighten the `listApplicants` composition label.** For US-3.5 the design is admirably explicit that the U5 read is "FE 화면 조합 (백엔드 U4→U5 아님)". The symmetric `U3→U4` listApplicants read is labeled only "ADR-007 R-1"; stating equally explicitly whether U3 consumes it via FE composition or a shared read port would keep the acyclic story airtight and match the S1 pin recommended in the units-generation review.

Verdict: READY
