# Business Logic Model — U9 Admin/Monitoring (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 스토리 US-9.1/9.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U9), requirements.md(FR9.1·FR9.2 TBD), components.md(C8·admin feature), component-methods.md(AdminQueryService getApprovalQueues/getMonitoring), services.md(관리자 조회 read 조합), U1(Role·MeetingStatus·CompletionStatus·ErrorPayload). 승인 큐·모니터링 read 조합·FE 화면 정의. -->

## 개요

U9의 관리자 조회 워크플로우(승인 큐·운영 현황)와 FE 화면을 정의한다. 소유 데이터 없이 U3/U4/U5 read 조합. 관리자 전용. U1 CC-1 상속.

## 워크플로우

### W1. 승인 큐 집계 (US-9.1 / getApprovalQueues)

```
getApprovalQueues(adminId):
  Principal.role==ADMIN? 아니면 403
  creation        = U3.listByStatus(PENDING_APPROVAL)          # read
  recruitConfirm  = U3.listByStatus(RECRUITING, 모집종료)       # read
  start           = U3.listByStatus(READY_TO_START)            # read
  meetingComplete = U3.listByStatus(IN_PROGRESS) ∩ U5.allScheduledSessionsEnded  # read 조합
  menteeComplete  = U5.listByCompletion(COMPLETION_CANDIDATE)  # read
  return { creation[], recruitConfirm[], start[], meetingComplete[], menteeComplete[] }
```

- 각 큐 항목 클릭 → 소유 Service 액션 호출(U3 approveCreation/confirmRecruitment/approveStart/completeMeeting, U5 approveMenteeCompletion). U9는 큐 제공만.

### W2. 운영 현황 모니터링 (US-9.2 / getMonitoring)

```
getMonitoring(adminId):
  Principal.role==ADMIN? 아니면 403
  for each meeting (U3 read):
     status·정원(capacity) = U3 read (capacity는 meeting 소유 필드 = U3)
     신청 수               = U4 read (count, U4 소유)
     출석율·수료진행        = U5 read(computeCompletion/집계)
  return MeetingMonitorRow[]
```

- 세션 기준 출석율(U5), 수료 후보/확정 수(U5). 읽기 전용.

## FE 화면 (admin feature)

- **승인 큐 대시보드:** 5개 큐(①개설·모집확정·②시작·③모임완료·④멘티수료) 탭/섹션. 각 항목에 승인/반려/확정 액션(소유 Service 호출). 관리자 전용 라우트.
- **운영 현황 모니터링:** 모임별 상태·신청/정원·출석율·수료 진행 테이블. (집계 지표는 범위 밖 — 표시 안 함.)
- 접근성(CC-2)·목록 상태(CC-3 로딩/빈/에러) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3(모임 상태별 목록·정원 capacity), U4(신청 수), U5(세션종료·수료·출석율). U9 depends_on U3/U4/U5(DAG 최상위) — 전부 정방향 read. 소유 write 없음. (선언된 U8 의존은 이번 범위에서 미사용 — 향후 피드백-뷰용 예약; 미사용 선언은 순환 아님.)
- **read-out:** 없음(어느 Unit도 U9 read 안 함) → 순환 불가.
- **액션 위임:** 승인/확정 액션은 U3/U5 Service. U9는 큐·현황 조회만.

## 에러·엣지 케이스

- 비관리자 접근 → 403.
- 소스 Unit read 실패 → 5xx 명시적(부분 실패 시 해당 큐/행만 오류 표시, 전체 대시보드 실패 회피 — graceful).
- 빈 큐 → 빈 배열(정상).

## Assumptions & Open Questions

- **[decided]** 관리자 전용, U9=조회 조합만, 집계 지표(FR9.2) 범위 밖(US-9.3 Won't).
- **[assumption]** recruitConfirm 판정(모집기간 종료), 큐 최소 필드.
- **[open]** U3 `listByStatus`·U5 `listByCompletion`/`allScheduledSessionsEnded`·U4 count read 포트 시그니처 정합(U3/U4/U5 계약).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U9 Admin/Monitoring, kind=service / read-only query layer; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts and the U1 shared kernel)

Verdict: READY

I walked in assuming the queue-to-status mapping was wrong, an approval action had leaked into U9, a cross-unit read would close a cycle, or a referenced port was silently invented. I could not sustain any of those into a blocking finding. Every queue resolves to a real U1 enum value, the shape is byte-identical to the contract, all write actions delegate to their owning units, and the one non-existent-yet read ports are all honestly tagged `[open]`.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Queue → status mapping — PASS.** All five queues map to the correct U1 shared-kernel values: `creation`=`PENDING_APPROVAL`(개설신청, ①), `recruitConfirm`=`RECRUITING`(모집중, +모집기간 종료 sub-condition), `start`=`READY_TO_START`(시작대기, ②), `meetingComplete`=`IN_PROGRESS`(진행중) ∩ U5 전 세션 종료(③), `menteeComplete`=`COMPLETION_CANDIDATE`(수료후보, ④). All four MeetingStatus values and the one CompletionStatus value exist in U1 domain-entities.md (`MeetingStatus`: PENDING_APPROVAL/RECRUITING/READY_TO_START/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED; `CompletionStatus`: NOT_COMPLETED/COMPLETION_CANDIDATE/COMPLETED). No invented status literal; "모집기간 종료" is a computed time condition (scheduler-less, ADR-005), correctly tagged `[assumption]`, not a phantom enum value.
- **③ meetingComplete semantics — PASS.** `IN_PROGRESS AND allScheduledSessionsEnded` is consistent with rev-mk/FR7.2/US-7.3 (관리자 직접 완료, 멘토 신청 없음) and with component-methods `completeMeeting` (precondition "전 세션 종료" verified via C4 read, write single-owned by C2, ADR-007 R-2). U9 composing the same read for the queue-display view is a read aggregation, not a second writer.
- **Shape fidelity — PASS.** `{creation[], recruitConfirm[], start[], meetingComplete[], menteeComplete[]}` matches component-methods C8 `getApprovalQueues` exactly (5 fields, same names, same order). `getMonitoring -> MeetingMonitorRow[]` matches, and MeetingMonitorRow is defined in domain-entities.md. Both C8 methods are present; no drift, no extra/missing method.
- **Action vs query separation — PASS.** U9 has no write in any of the three artifacts. All approval actions delegate to owning units and every named method resolves to a passed contract: `approveCreation`/`confirmRecruitment`/`approveStart`/`completeMeeting` → `MeetingApprovalService` (C2→U3); `approveMenteeCompletion` → `CompletionService` (C4→U5). recruitConfirm-as-a-queue with the action delegated to U3 `confirmRecruitment` is consistent with OQ1/A2 resolution (US-3.4).
- **Acyclicity — PASS.** U9 `depends_on U1,U2,U3,U4,U5,U8` matches the unit-of-work.md summary table; all reads are DAG-forward. No unit lists U9 in its `depends_on` → U9 is the DAG top, nothing reads U9, so the read composition cannot close a cycle. Reads go through domain Service interfaces (BR-U9-5 forbids direct table access), consistent with the modular-monolith in-process Service call contract (services.md) and ADR-007.
- **Admin-only authorization — PASS.** BR-U9-1 gates every U9 query on `Principal.role == ADMIN` else 403, correctly inheriting U1 BR-U1-5 (role != ADMIN → 403) and the CC-1 error contract.
- **Aggregate metrics out of scope — PASS.** BR-U9-4 and domain-entities "범위 밖" exclude 개설 대비 승인 수/모집 충족률/평균 출석율/수료율/만족도/멘토 재개설률, correctly citing FR9.2 (TBD 이월) and US-9.3 (Won't). Story-map confirms US-9.3 = Won't (not unmapped).
- **Story coverage — PASS.** US-9.1 (승인 큐, W1/getApprovalQueues) and US-9.2 (모니터링, W2/getMonitoring) both covered; US-9.3 correctly excluded.
- **Epistemic honesty — PASS (the sharpest check).** The four read ports U9 needs — U3 `listByStatus`, U5 `listByCompletion`, U5 `allScheduledSessionsEnded`, U4 `count` — do NOT exist in component-methods.md (which ships `listRecruiting`/`listMyMeetings`, `computeCompletion`, `listSessions`, `listApplicants`). U9 does not pretend they exist: all four are explicitly listed under `[open]` "read 포트 시그니처 정합" in all three artifacts. `getMyAttendance` and `computeCompletion` (the ones it does claim as existing) DO resolve to C4. No silent promotion of `[open]`/`[assumption]` to `[decided]`.
- **Sensors — PASS.** required-sections: business-logic-model.md has 6 H2 (개요/워크플로우/FE 화면/통합 지점 요약/에러·엣지 케이스/Assumptions), business-rules.md 9, domain-entities.md 5 — all ≥2. upstream-coverage: each file's header references unit-of-work, unit-of-work-story-map, requirements, components, component-methods, and services. No fenced TS/JS/TSX (the workflow pseudocode is untagged), so linter/type-check have nothing to flag.

### Suggestions (non-blocking; resolve when the read ports are pinned downstream)

- **S1 — `정원`(capacity) source attribution is off by one unit.** In W2 and the MeetingMonitorRow table, "신청 수/정원" is attributed to a **U4** read. Applicant count is U4-owned, but **capacity is a `meeting` field owned by U3** (component-methods `MeetingCreateRequest.capacity`, C2 ownership) — and U4's own unit-of-work note says "정원은 U3 read". Reading 정원 from U4 contradicts U9's own BR-U9-5 ("소유 Unit Service read"). Since MeetingMonitorRow already reads U3 for "모임 기본·status", fold 정원 into that U3 read and keep only 신청 수 on U4. Non-blocking because it sits inside the already-`[open]` read-port item and a developer can resolve it from the ownership rule.
- **S2 — Declared U2/U8 dependencies are unexercised by in-scope queries.** U9 declares `depends_on U1,U2,U3,U4,U5,U8`, but the integration summary only reads U3/U4/U5 (and correctly narrows to "U3/U4/U5" there). U2 (auth Principal) is implicit and fine; **U8 (survey/feedback) is never read** in scope — it would only matter for feedback viewing / aggregate metrics, which are out of scope. Harmless (a declared-but-unused dependency creates no cycle), but a one-line note that U8 is reserved for future feedback-view would keep the DAG-declaration and the exercised-reads consistent.
- **S3 — Pin the four read-port signatures in the owning units' functional-design.** `U3.listByStatus(status[, recruitEnded])`, `U5.listByCompletion(COMPLETION_CANDIDATE)`, `U5.allScheduledSessionsEnded(meetingId)`, `U4` applicant-count are all `[open]`. They are the item that makes U9 buildable; ensure U3/U4/U5 expose them as read ports (per ADR-007) rather than U9 reaching into their tables. Correctly flagged here — this is a downstream cross-unit action item, not a U9 defect.
- **S4 — recruitConfirm 판정 criterion.** "모집기간 종료" is `[assumption]`; when U3 pins the recruit-period-end read, confirm U9's queue predicate matches U3's authoritative definition to avoid a queue that lists meetings U3 would reject for confirmation.

Verdict: READY
