# Business Logic Model — U8 Survey/Feedback (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U8 Survey/Feedback(service). 스토리 US-3.6/8.1/8.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U8), requirements.md(FR3.6·FR8.1/8.2), components.md(C7·survey feature), component-methods.md(PreSurveyService submitAnswers/getAnswers·FeedbackService submitFeedback/listFeedback), services.md, U1(ErrorPayload/Principal). 사전설문 응답 게이팅·피드백·열람 권한·FE 화면 정의. -->

## 개요

U8의 사전설문 응답(②후), 과정 설문 제출, 피드백 열람 워크플로우와 FE 화면을 정의한다. 문항 틀은 U3 read. U1 CC-1 상속.

## 워크플로우

### W1. 사전설문 응답 (US-3.6 / submitAnswers)

제출(submitAnswers)만 IN_PROGRESS 게이팅. 조회(getAnswers, W2)는 status 게이팅 없이 인가 기준만(모임 COMPLETED 후에도 멘토·관리자 열람 가능).

```
submitAnswers(menteeId, meetingId, Answer[]):
  1. 참여자(U4 APPLIED read)? 아니면 403
  2. 모임 status==IN_PROGRESS(②후)? 아니면 409 PRESURVEY_NOT_OPEN  # 제출 마감=COMPLETED 시점
  3. questions = U3.getQuestions(meetingId)  # 문항 틀 read
  4. 필수 문항 응답 검증(누락 400)
  5. upsert survey_answer(questionId, menteeId, answerText)  # unique(question,mentee)
  6. return
```

### W2. 사전설문 응답 열람 (getAnswers)

```
getAnswers(requesterId, meetingId, menteeId):
  소유 멘토/관리자(자기 모임/전체) 또는 본인? 아니면 403
  → Answer[]
```

- U3 운영 허브 화면이 이 엔드포인트를 read 조합(FE, U3→U8).

### W3. 과정 설문·피드백 제출 (US-8.1 / submitFeedback)

```
submitFeedback(menteeId, meetingId, FeedbackRequest):
  참여 멘티 본인 + 모임 IN_PROGRESS/COMPLETED? 아니면 403/409
  → Feedback insert
```

### W4. 피드백 열람 (US-8.2 / listFeedback)

```
listFeedback(requesterId, meetingId):
  requester가 소유 멘토(자기 모임) 또는 관리자? 아니면 403 (타 모임 멘토 403)
  → Feedback[]
```

## FE 화면 (survey feature)

- **사전설문 응답(멘티):** ②시작 후에만 노출. 멘토 구성 문항(U3 read) 폼 → 응답 제출. ② 전이면 "시작 후 응답 가능" 안내.
- **과정 설문(멘티):** 피드백 제출 폼(진행/완료 모임).
- **피드백 열람(멘토/관리자):** 자기 모임(멘토)·전체(관리자) 피드백·사전설문 응답 조회. 타 모임 멘토 접근 403.
- 접근성(CC-2)·목록 상태(CC-3) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3(모임 상태 ②후 게이팅·문항 틀 getQuestions·소유 멘토), U4(참여자). U8 depends_on U3/U4(DAG) — 정방향 비순환.
- **read-out:** 사전설문 응답(getAnswers)은 U3 운영 허브(FE 조합)·U9 모니터링 read.
- **write:** survey_answer/feedback(U8 소유). 문항 틀(survey_question)은 U3 소유(U8은 read만).

## 에러·엣지 케이스

- ② 전 사전설문 응답 → 409 PRESURVEY_NOT_OPEN.
- 비참여자 응답/피드백 → 403.
- 타 모임 멘토 피드백 열람 → 403.
- 필수 문항 미응답 → 400.
- 재응답 → 갱신(멱등적 upsert).

## Assumptions & Open Questions

- **[decided]** 사전설문 ②후(FR3.6), 타 모임 멘토 열람 403.
- **[assumption]** 필수 미응답 400(OQ7), 재제출 갱신, 과정 설문 제출 시점.
- **[open]** 과정 설문 문항 구조, U3 상태·문항 read·U4 참여자 read 포트 시그니처.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — re-review iteration 2 (Unit U8 Survey/Feedback, kind=service; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts + U1 shared kernel)

Verdict: READY

Iteration 1 returned NOT-READY on two self-contradictions between business-rules.md and business-logic-model.md over the two permission/gating boundaries this unit exists to own (B1 pre-survey read gating, B2 feedback read permission), plus non-blocking S3/S4. I re-hunted both boundaries adversarially, re-verified the passed contracts, and swept for regressions. Both blockers are resolved cleanly and no new contradiction was introduced.

### Prior blockers — resolution verified

- **B1 — RESOLVED.** The submit/read split is now stated consistently across all four loci and matches the contract.
  - `business-rules.md` **BR-U8-1** now gates **only** submit: "응답 제출(submitAnswers)은 모임 status=IN_PROGRESS(②시작 이후)에만 ... 제출 마감은 COMPLETED 시점." Read is carved out explicitly: "응답 조회(getAnswers)는 status 게이팅 없이 인가 기준만 — 소유 멘토·관리자·본인 ... 모임이 COMPLETED 되어도 멘토·관리자는 계속 열람 가능." The IN_PROGRESS-only read gate that broke FR3.6/hub/monitoring is gone.
  - `business-logic-model.md` **W1** is annotated to the same effect ("제출(submitAnswers)만 IN_PROGRESS 게이팅. 조회(getAnswers, W2)는 status 게이팅 없이 인가 기준만"); **W2** is unchanged and correct (authorization-only). **BR-U8-2** and the **BR-U8-5** summary agree (read = 소유 멘토·관리자·본인, status 게이팅 없음).
  - Contract fidelity: `component-methods.md` `getAnswers(requesterId, meetingId, menteeId) -> Answer[]` carries `menteeId`, so the owner-mentor/admin/self read is expressible as signed. Mentor/admin retain read after `COMPLETED` — FR3.6, the U3 operational-hub read composition, and the U9 monitoring read-out are all satisfied. No document-wins ambiguity remains.

- **B2 — RESOLVED.** The mentee-own feedback read path is removed everywhere; feedback read is owner-mentor + admin only, matching the signature and the requirement.
  - `business-rules.md` **BR-U8-4** now reads "listFeedback(requesterId, meetingId)는 소유 멘토(자기 모임)·관리자만 열람 → 타 모임 멘토 403 ... 멘티 열람 경로 없음," and explicitly notes the signature limitation: "listFeedback는 meetingId 단위 반환이며 menteeId 파라미터가 없어 '본인 것만' 조회를 표현할 수 없다 — 멘티 본인 열람은 스코프 밖(FR8.2)." The **BR-U8-5** summary matches ("피드백 열람 ... 소유 멘토·관리자만, 멘티 본인 열람 경로 없음, FR8.2"). Mentee is submit-only.
  - `business-logic-model.md` **W4** is unchanged and now consistent (owner-mentor/admin only, other-mentor 403, no mentee path).
  - Contract fidelity: `component-methods.md` `listFeedback(requesterId, meetingId) -> Feedback[]` = "멘토(자기 모임)·관리자 열람, 타모임 멘토 403" (no `menteeId`, no mentee) and FR8.2/US-8.2 are mentor+admin only. BR now aligns with W4 and the contract; the scope creep is gone.

- **S3 — RESOLVED.** business-rules.md's source header now cites `services.md(REST/에러 경계·오케스트레이션)`, so all three artifacts reference the full `consumes:` set. upstream-coverage sensor satisfied.
- **S4 — RESOLVED.** The pre-survey submit cutoff is now an explicit decision, not an artifact of exact-equality: BR-U8-1 "제출 마감은 COMPLETED 시점(그 이후 제출 불가)" and W1's inline "# 제출 마감=COMPLETED 시점."

### Regression sweep (what I re-checked, and why it still passes)

- **Cross-unit acyclicity (U8→U3, U8→U4) — PASS, unchanged.** `U8 depends_on U1,U2,U3,U4`; U3/U4 take no dependency on U8. Backend reads (U3 status + `getQuestions` + owner-mentor, U4 `APPLIED` participant) are forward edges; the `getAnswers` read behind the U3 hub is FE composition at the client (U3→U8), not a backend edge; U9's read-out is covered by `U9 depends_on ...,U8`. Graph stays acyclic. No edit touched the dependency shape.
- **Method fidelity to component-methods C7 — PASS.** All four names/signatures (`submitAnswers`, `getAnswers`, `submitFeedback`, `listFeedback`) still match W1–W4 verbatim; the B1/B2 edits changed only prose permission/gating semantics, not signatures.
- **Submit gating & timing — PASS.** W1 pre-survey submit gated to `IN_PROGRESS` → `409 PRESURVEY_NOT_OPEN` (cutoff at `COMPLETED` now explicit); W3/BR-U8-3 feedback submit `IN_PROGRESS`/`COMPLETED` participant-mentee, matching component-methods. `APPLIED` participant marker still coherent (cancel forbidden after ②).
- **Pre-survey mentee-own read — PASS (not a re-introduced B2).** The 본인 path survives only for `getAnswers`, which carries `menteeId` and can express it; iteration 1 flagged the mentee path only for feedback (where the signature cannot). No asymmetry defect.
- **Ownership / CC-1 / story coverage — PASS, unchanged.** `survey_answer`+`feedback` owned by U8, `survey_question` U3-read-only, `unique(question_id, mentee_id)` intact; 400/403/404/409 `PRESURVEY_NOT_OPEN` + `ErrorPayload`/Korean inherit U1 CC-1; US-3.6 (W1/W2), US-8.1 (W3), US-8.2 (W4) all mapped.
- **Epistemic status — PASS, improved.** The previously untagged mentee-own-feedback assertion (the B2 caveat) is gone; remaining `[assumption]`/`[open]` tags (required-question 400/OQ7, upsert, course-survey timing & structure, U3/U4 read-port signatures) are honest.
- **Sensors — PASS.** required-sections: business-logic-model.md 7 H2, business-rules.md 8 H2, domain-entities.md 5 H2 (all ≥2). No fenced TS/JS/TSX → linter/type-check inert. upstream-coverage: all three artifacts now reference the full consumes set including `services.md`.

### Residual suggestions (non-blocking, downstream — carried from iteration 1)

- **S1 — Pin the U4 participant read port (and `APPLIED` status vocabulary) when U4's contract firms up**, so "participant" resolves to a U4-owned value rather than a literal chosen here. Read-port signature already `[open]`.
- **S2 — Confirm the U3 `getQuestions` / `SurveyQuestion` shape exposes the `required` flag** that W1 step 4 / BR-U8-1 depend on, when that signature lands. Correctly `[open]`.

Verdict: READY
