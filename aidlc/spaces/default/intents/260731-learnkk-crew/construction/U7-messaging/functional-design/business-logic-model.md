# Business Logic Model — U7 Messaging (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U7 Messaging(service). 스토리 US-5.1(unit-of-work-story-map.md). 출처: unit-of-work.md(U7), requirements.md(FR5.1~5.3), components.md(C6·messaging feature), component-methods.md(MessageService send/listThreads/getThread/unreadCount), services.md, U1(ErrorPayload/Pagination/Principal). 쪽지 발신·미확인·스레드·권한·FE 화면 정의. -->

## 개요

U7의 쪽지 워크플로우(발신 권한·미확인 뱃지·스레드), FE 화면을 정의한다. 권한은 U3/U4 read 조합. 인앱 폴링(FR5.2). U1 CC-1 상속.

## 권한 헬퍼 (공통 전처리)

```
canMessage(senderId, recipientId):
  s = Principal(senderId)
  if s.role == ADMIN: return true
  if s.role == MENTOR:
     recipient가 관리자면 true
     아니면 U4/U3 read: sender 소유 모임 중 recipient가 활성(비취소) 등록 관계인 모임 존재? → true, else false
        # 'APPLIED' 단일 리터럴 아님 — ②시작 후에도 유지되는 활성 등록 관계(정확 상태집합은 U4 계약, S1)
  if s.role == MENTEE:
     recipient가 관리자면 true
     아니면 U4/U3 read: sender 신청 모임 중 recipient가 그 모임의 멘토? → true, else false
  # U7→U3/U4 read, 비순환(U3/U4는 U7 비의존). Service 인터페이스 경유.
```

## 워크플로우

### W1. 발신 (US-5.1 / send)

```
send(senderId, recipientId, body):
  1. senderId == recipientId → 400
  2. canMessage(senderId, recipientId)? 아니면 403 MESSAGE_FORBIDDEN
  3. body 검증(비어있음 400)
  4. thread = 기존 스레드(정규화 pair) 조회 or 생성
  5. Message insert(threadId, senderId, body, createdAt); thread.lastMessageAt=now
  6. return MessageResponse
```

### W2. 스레드 목록·조회 (listThreads / getThread)

```
listThreads(userId): 본인 참여 스레드(최신순) → ThreadSummary[](미확인 수 포함)
getThread(userId, threadId):
  본인 참여 스레드? 아니면 403
  메시지 페이지 반환 + 수신 메시지 readAt=now(확인 처리)
```

### W3. 미확인 수 (unreadCount) — 폴링

```
unreadCount(userId): sum(readAt IS NULL AND senderId != userId) 전 스레드 → int
```

- FE가 주기적 폴링(뱃지 갱신, FR5.2). 서버는 무상태 조회.

## FE 화면 (messaging feature)

- **쪽지함:** 스레드 목록(상대·최근 메시지·미확인 뱃지). 진입 시 스레드 대화(시간순), 하단 입력.
- **미확인 뱃지:** 앱 셸 네비/쪽지 탭에 미확인 수 뱃지 — `unreadCount` 폴링(주기 [assumption]).
- **발신 대상 선택:** 권한 있는 상대만 노출(멘토=자기 모임 멘티·관리자 / 멘티=자기 모임 멘토·관리자). 서버 재검증(403).
- 접근성(CC-2)·목록 상태(CC-3) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3(모임 소유)·U4(신청 관계) — `canMessage` 관계 판정. U7→U3/U4 비순환(Service read).
- **write:** message_thread/message(U7 소유).
- **read-out:** 없음(U7 데이터를 타 Unit이 read하지 않음).

## 에러·엣지 케이스

- 무관계 멘토→멘티 발신 → 403 MESSAGE_FORBIDDEN.
- 자기 자신 발신 → 400.
- 타인 스레드 열람 → 403.
- 빈 본문 → 400.
- **관계 해지(등록 취소) 후 비대칭(S2):** 기존 스레드 열람/목록은 참여자면 가능하나, 신규 발신은 canMessage 재검증으로 403 가능(파일럿 수용).
- 폴링 부하: 파일럿 규모라 미확인 count 쿼리 경량(인덱스).

## Assumptions & Open Questions

- **[decided/OQ2]** 스레드형(채팅형 미채택). 폴링·푸시 없음.
- **[assumption]** 폴링 주기, 멘티 발신 대상, 스레드 pair 정규화.
- **[open]** `canMessage`용 U3/U4 관계 read 포트 시그니처(U3/U4 계약 정합). U7→U4 비순환.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U7 Messaging, kind=service; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts and the U1 shared kernel).

Verdict: READY

I walked in trying to break three things: the messaging permission boundary, the U7→U3/U4 read edges (looking for a cycle), and the OQ2 resolution. I could not sustain any of them into a blocking finding. Every cross-unit claim resolves to a consumed contract, the read edges are provably acyclic and forward-only, and the epistemic tagging is honest.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Acyclicity of U7→U3 and U7→U4 reads — PASS (the core claim).** From unit-of-work.md's summary table: `U3 depends_on=[U1,U2]`, `U4 depends_on=[U1,U2,U3]`. Neither names U7, so no path U3→…→U7 or U4→…→U7 exists — adding U7→U3 and U7→U4 read edges creates **no cycle**. The design's claim "U3·U4는 U7에 의존하지 않으므로 비순환" is directly verifiable and correct. Stronger than the U3↔U4 case (ADR-007 R-1, genuinely bidirectional): U7→U4 is a **forward-only** edge (U4 sits before U7 in topological order after U3), so U7 can compile against U4's Service interface with zero back-edge risk. Routing "via Service 인터페이스 경유(모듈 소유 준수)" is consistent with the components.md module-ownership rule and the ADR-007 read-port pattern.
- **U7→U4 edge beyond declared depends_on — correctly surfaced, not silently taken.** unit-of-work.md's summary table lists `U7 depends_on=[U1,U2,U3]` (omits U4), but the U7 note explicitly says "멘토-멘티 관계 권한은 U3/U4 read" — naming U4. The design resolves this table/note tension in favor of the note, flags the expansion in all three artifacts, and tags the read-port as `[open]`. This is exactly the honest cross-unit handling the boundary demands.
- **Permission boundary (BR-U7-1) traces to contracts — PASS.** ADMIN→all, MENTOR→(own-meeting mentee | admin), MENTEE→(applied-meeting mentor | admin) is a legitimate refinement of FR5.1's "멘토↔멘티, 관리자↔멘토/멘티" — and the narrowing to a mentoring *relationship* is exactly what the unit-of-work U7 note anticipates ("관계 권한은 U3/U4 read"), so it is **not a silent promotion**. The mentee send-target rule is correctly tagged `[assumption]`. Role checks read U2 Principal `{userId, role}` (U1 kernel, `AuthService.validateSession`), relationship checks read U3 (`listMyMeetings`/`getMeeting`) + U4 (`listApplicants`/`listMyEnrollments`) — all concrete methods that exist in component-methods.md, so a developer can compose `canMessage` today.
- **Method fidelity to component-methods C6 — PASS.** All four MessageService methods are faithful: `send`(W1, 403 MESSAGE_FORBIDDEN on violation), `listThreads`(W2), `getThread`(W2, marks read), `unreadCount`(W3, polled int). Pagination on getThread is a valid refinement of U1's Pagination convention, not a divergence from the `Message[]` signature.
- **Unread/polling (BR-U7-2) — PASS.** `readAt IS NULL AND senderId != userId` count; `unreadCount` summed across threads for the in-app badge; `getThread` sets `readAt=now` on received messages; no push/email (FR5.2). Matches FR5.2 verbatim ("인앱 확인 방식 … 폴링/새로고침 … 푸시·이메일·SMS 없음").
- **OQ2 resolution — PASS/defensible.** OQ2 is assigned to application-design/functional-design for resolution; functional-design has the authority. Retaining thread-based and deferring chat-style matches the upstream default (requirements A3 "기본 가정은 인앱 쪽지", components.md C6 "스레드형 기본", FR5.3 "미확정 설계 여지"). Marked `[decided/OQ2]` — correct.
- **Story/requirement coverage — PASS.** US-5.1 → U7 (W1/W2/W3 + FE 쪽지함/뱃지); FR5.1 → BR-U7-1; FR5.2 → BR-U7-2; FR5.3 → BR-U7-4/OQ2. No orphaned requirement.
- **Epistemic status — PASS.** OQ2 `[decided]`; mentee send-target, thread-per-pair, polling interval `[assumption]`; relationship read-port signature `[open]` pending "U3/U4 계약 정합". No silent promotion of the mentee rule beyond FR5.1.
- **Sensors — PASS.** required-sections: business-logic-model.md 7 H2, business-rules.md 9 H2, domain-entities.md 5 H2 (all ≥2; no template override in play). upstream-coverage: the sensor checks the **union** of the stage's deliverables, and across the three files all six consumed artifacts are referenced (unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services). linter/type-check: pseudo-code uses bare ``` fences with no `ts`/`js`/`tsx` language tag, so the code-shape sensors bind to nothing.

### Suggestions (non-blocking)

- **S1 — The `APPLIED` enrollment-status literal is a correctness risk; pin its meaning during U3/U4 contract alignment.** BR-U7-1 gates mentor→mentee on "자기 모임에 **APPLIED** 신청한 멘티". Enrollment status is owned by U4 (not in the U1 kernel, which owns only MeetingStatus/CompletionStatus/Role). If a developer implements this as a literal `enrollment.status == 'APPLIED'`, messaging could **silently break once a meeting starts** — after 모집확정/②시작 the mentee's enrollment plausibly leaves an "applied" state (FR3.5: post-② the mentee can no longer leave), which is exactly when mentor↔participant messaging matters most. The intent reads as "has an active (non-cancelled) enrollment in the mentor's meeting", not a single status value. This falls under the existing `[open]` "U3/U4 계약 정합" item, so it is not blocking — but the design should state the predicate as *active-enrollment relationship* (or name the exact U4 status set) rather than a lone `APPLIED` literal, so the read-port contract is unambiguous.
- **S2 — Read-vs-reply asymmetry on relationship revocation is undocumented.** `getThread`/`listThreads` gate only on thread participation, while `send` re-checks `canMessage`. If an enrollment is cancelled after a thread exists, a participant can still read the thread but a reply may be rejected 403. Reasonable for a pilot, but one line in the edge-case list would remove the ambiguity.
- **S3 — Reconcile the upstream table/note mismatch (upstream nit).** unit-of-work.md's summary table omits U4 from U7's `depends_on` while its U7 note names "U3/U4 read". U7's design handles this correctly; a one-line note in units-generation that read-only edges are intentionally not in the `depends_on` column would prevent future confusion. Not a U7 defect.

Verdict: READY
