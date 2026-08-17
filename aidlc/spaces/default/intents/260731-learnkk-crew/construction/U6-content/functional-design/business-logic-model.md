# Business Logic Model — U6 Content (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U6 Content(service). 스토리 US-4.1a/4.1b/4.2/4.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U6), requirements.md(FR4.1~4.5·A1 20MB), components.md(C5·content feature), component-methods.md(PostService createPost/listPosts·AttachmentService upload/download·NoticeService postNotice), services.md(BLOB 스트리밍), U1(ErrorPayload/Pagination). 게시글·첨부·공지 워크플로우·참여자 인가·FE 화면 정의. -->

## 개요

U6의 게시글/첨부/공지 워크플로우, 참여자 인가(U3/U4 read 조합), FE 화면을 정의한다. 첨부는 bytea BLOB(ADR-004). U1 CC-1 상속.

## 참여자 인가 헬퍼 (공통 전처리)

```
assertParticipant(requesterId, meetingId):
  isMentor = (U3.getMeeting(meetingId).mentorId == requesterId)   # 기존 C2 메서드 사용
  isMentee = U4.isParticipant(meetingId, requesterId)            # U4 신규 read 포트(계약 추가 필요)
  isAdmin  = Principal.role == ADMIN
  not (isMentor or isMentee or isAdmin) → 403 CONTENT_FORBIDDEN
```

- **계약 근거(S1):** 멘토 소유 판정은 기존 `MeetingService.getMeeting(meetingId).mentorId`(component-methods C2)로 충족. 멤버십 판정은 component-methods C3 `EnrollmentService`에 현재 `isParticipant`/`isEnrolled`가 **없다** — 자기-참여 확인은 `listMyEnrollments(requesterId)`로도 가능하나, U6의 인가 read를 위해 **U4 계약에 `EnrollmentService.isParticipant(meetingId, userId)` read 포트를 추가**해야 한다(U4 functional-design 정합 항목).
- U6→U4/U3 read는 각 Service 인터페이스 경유(테이블 직접 접근 아님). U4/U3는 U6에 의존하지 않아 비순환.
- **참여 상태(S4):** 게시글·공지는 ②시작(IN_PROGRESS) 이후에만 존재하므로 이 시점 멤버십은 확정 상태다. 콘텐츠 열람을 부여하는 신청 상태는 **APPLIED(②시작 시점 참여 확정 멘티)** — 취소(CANCELLED)는 열람 불가. 정확한 상태 집합은 U4 계약과 정합(현재 U4는 APPLIED/CANCELLED만).

## 워크플로우

### W1. 게시글 작성 (US-4.1a / createPost)

```
createPost(mentorId, meetingId, {week, body}):
  1. 소유 멘토 확인(U3 read mentorId==mentorId, role=MENTOR) 아니면 403
  2. body 필수·week 범위 검증 실패 400
  3. Post insert(authorId, createdAt)
  4. return PostResponse
```

### W2. 첨부 업로드 (US-4.1b / upload)

```
upload(mentorId, postId, file):
  1. 소유 멘토 확인(게시글 소유) 아니면 403
  2. contentType 화이트리스트? 아니면 400 ATTACHMENT_TYPE_NOT_ALLOWED
     (매직넘버 확인 권고)
  3. sizeBytes > 20MB? → 400 ATTACHMENT_TOO_LARGE
  4. bytea로 저장 + 메타(fileName·contentType·sizeBytes·uploaderId)
  5. return AttachmentResponse
```

### W3. 열람·다운로드 (US-4.2 / listPosts, download)

```
listPosts(meetingId, requesterId):
  assertParticipant(requesterId, meetingId)  # 403 if not
  → Post 목록(주차·본문·첨부 메타) 페이지
download(requesterId, attachmentId):
  attachment 조회(404) → assertParticipant(requesterId, attachment.post.meetingId)
  → bytea 스트리밍 응답(Content-Type, Content-Disposition)
```

- **스트리밍:** bytea는 응답 스트림으로 전달(20MB 상한이라 전량 로드 수용, ADR-004). OOM 완화: 동시 대용량 다운로드 제한은 [open].

### W4. 공지 (US-4.3 / postNotice)

```
postNotice(mentorId, meetingId, {body}): 소유 멘토(403) → Notice insert → return
```

## FE 화면 (content feature)

- **자료실(게시글):** 주차별 게시글 목록·본문·첨부 다운로드(참여자만). 멘토는 작성/첨부 UI. 비참여자 접근 시 403 안내.
- **공지:** 공지 목록. 멘토 작성 UI.
- 첨부 업로드: 형식/크기 클라이언트 사전 검증(20MB, 화이트리스트) + 서버 재검증.
- 접근성(CC-2)·목록 상태(CC-3) 상속.

## 통합 지점 요약

- **백엔드 read-in:** U3 멘토 소유(작성 인가), U4 참여자(열람 인가) — Service read, U6→U3/U4 비순환.
- **write:** post/post_attachment/notice(U6 소유).
- **read-out:** 없음(U6 데이터를 타 Unit이 read하지 않음).

## 에러·엣지 케이스

- 비참여자 열람/다운로드 → 403 CONTENT_FORBIDDEN.
- 허용 외 형식 업로드 → 400 ATTACHMENT_TYPE_NOT_ALLOWED.
- 20MB 초과 → 400 ATTACHMENT_TOO_LARGE.
- 타 멘토가 남의 모임 게시글 작성 → 403.
- 첨부 없는 게시글 작성 → 정상(본문만).

## Assumptions & Open Questions

- **[decided/OQ4]** bytea(ADR-004)·20MB(A1)·형식 화이트리스트.
- **[assumption]** 매직넘버 검증, 첨부 개수 상한, 관리자 열람.
- **[open]** 대용량 동시 다운로드 제한, 편집/삭제 정책. **U4 계약에 `isParticipant(meetingId,userId)` read 포트 추가 필요**(S1) — 멘토 판정은 기존 getMeeting().mentorId로 충족.
- **[note]** bytea는 응답 스트림 수준만 스트리밍 — DB read는 전량 로드. services.md "OOM 회피"는 진정한 스트리밍이 아니라 20MB 상한+동시 다운로드 제한으로 충족(S2).
## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U6 Content, kind=service; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts + U1 shared kernel)

Verdict: READY

I walked in trying to break three things: a hidden cycle behind the participant-authorization read, a module-ownership violation (U6 reaching into U3/U4 tables), and a BLOB decision that either over-promises streaming or contradicts ADR-004/A1. I could not sustain any of them into a blocking finding. The dependency direction holds, the reads go through Service interfaces, and the bytea tradeoff is stated honestly rather than papered over.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Participant-authorization acyclicity — PASS.** unit-of-work.md summary table declares U6 `depends_on = [U1, U2, U3]`; U4 `depends_on = [U1, U2, U3]`; U3 `depends_on = [U1, U2]`. The artifacts add a U6→U4 read edge (participant membership) and lean on the existing U6→U3 read (mentor ownership). Tracing every path: U6→U4→U3→U2→U1 and U6→U3→U2→U1 — neither U3 nor U4 has any edge back to U6, and domain-entities.md's read-out row correctly states "없음(U6 데이터를 타 Unit이 read하지 않음)." No unit reads U6, so no back-edge can exist. **Acyclic, confirmed against the DAG.**
- **Anticipated-refinement framing — PASS.** The U6→U4 edge is not an unflagged expansion. unit-of-work.md's U6 note reads "참여자 열람 권한은 U4 read," and its per-unit constraints call out that read cross-references are resolved at functional-design. domain-entities.md correctly labels this a read-only extension of the declared `[U1,U2,U3]` anticipated by that note, not a new write dependency. This mirrors the ADR-007 pattern (read cross-refs resolved via ports/controller composition with writes single-owned).
- **Module ownership — PASS.** All three artifacts route the reads through Service interfaces (U3 mentor-ownership read, U4 `EnrollmentService` membership read), explicitly "테이블 직접 접근 아님, 모듈 소유 준수." Consistent with components.md ("교차 접근은 소유 모듈 Service 경유, 직접 테이블 접근 금지") and ADR-003. No U6-owned write crosses a boundary: writes are post/post_attachment/notice only.
- **BLOB / OQ4 resolution — PASS.** ADR-004 explicitly *deferred* the bytea-vs-LO choice to functional-design ("저장 타입 최종 선택(bytea vs LO)은 functional-design/구현에서 확정"). U6 resolves it to `bytea`, which is exactly the decision this stage was handed. 20MB matches A1 ("기본 제안 20MB … functional-design에서 최종 확정"). Format whitelist (PDF / png·jpg·jpeg·gif·webp / docx·xlsx·pptx / txt) is a faithful concretization of FR4.3's "문서 위주(PDF/이미지/오피스 등)." Magic-number check is correctly tagged `[assumption]` (a security add, not a requirement).
- **Streaming/OOM honesty — PASS.** The design does not hide bytea's full-load behavior. domain-entities.md: "bytea … 전량 로드가 수용 가능(파일럿). 진정한 스트리밍 필요 시 Large Object(LO)로 전환 [assumption]." business-logic-model W3: "20MB 상한이라 전량 로드 수용 … OOM 완화: 동시 대용량 다운로드 제한은 [open]." This is a truthful reconciliation of the upstream "스트리밍" language (services.md, component-methods AttachmentService) with the reality that bytea streams the *response body*, not the *DB read*. See S2 for the residual tension.
- **Post rules — PASS.** body required (Post.body NOT NULL, BR-U6-1, FR4.1); attachments 0..n with text-only allowed (FR4.1, "첨부 없이 글만 허용"); week-in-range tagged `[assumption]` (no explicit FR range rule, honestly flagged); metadata (fileName·contentType·sizeBytes·uploaderId·postId + week via post.week) covers FR4.4's 파일명·형식·크기·업로더·소속 게시글/주차 in full.
- **Method fidelity to component-methods C5 — PASS.** PostService.createPost(mentorId, meetingId, {week, body}) → W1; listPosts(meetingId, requesterId) → W3; AttachmentService.upload(mentorId, postId, file) → W2; download(requesterId, attachmentId) → W3; NoticeService.postNotice(mentorId, meetingId, {body}) → W4. All five signatures reflected without drift.
- **Story coverage — PASS.** US-4.1a (W1), US-4.1b (W2), US-4.2 (W3 listPosts + download), US-4.3 (W4). All four assigned stories covered.
- **Epistemic status — PASS.** OQ4 = `[decided]`; magic-number, attachment-count cap, admin-view all `[assumption]`; edit/delete policy and U3/U4 read-port signatures `[open]`. No silent promotion. Admin content-viewing (isAdmin in assertParticipant) is not backed by an FR — correctly held as `[assumption]` rather than asserted.
- **Sensors — PASS.** required-sections: business-logic-model 7 H2, business-rules 8 H2, domain-entities 5 H2 (all ≥2). upstream-coverage: the primary artifact's header references all six consumed artifacts — unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services. Pseudo-code blocks carry no TS/JS/TSX language tag, so linter/type-check have nothing to flag.

### Suggestions (non-blocking)

- **S1 — Bind the participant read port to an existing C3/C2 method, or declare a contract addition.** The pseudo-code in `assertParticipant` calls `U4.isEnrolled(meetingId, requesterId)` and `U3.isOwnerMentor(meetingId, requesterId)` as if settled, while Assumptions tags the signatures `[open]`. Checked against component-methods.md: **no `isEnrolled`/`isParticipant` method exists on `EnrollmentService`** (it exposes apply / cancel / listApplicants[mentor-gated, 403 for non-mentors] / listMyEnrollments). The mentor-ownership read resolves cleanly to `MeetingService.getMeeting(meetingId).mentorId`, and the membership read is satisfiable via `listMyEnrollments(requesterId)` for the self-participation case (which is the actual auth check). To keep U4/U3 construction from ever *not* exposing the needed shape, either cite those backing methods or explicitly state that a new read port (e.g. `EnrollmentService.isParticipant(meetingId, userId)`) must be added to the U4 contract. Leaving it purely `[open]` risks a silent cross-unit gap. Non-blocking because the port is named, acyclic, and satisfiable with the existing contract — but the confident pseudo-code overstates what the passed contract currently guarantees.
- **S2 — services.md's "스트리밍(OOM 회피)" rationale is only partially met by bytea.** services.md justifies BLOB streaming as *avoiding* OOM; bytea full-load does not avoid OOM, it bounds it (20MB × concurrency). The design honestly reframes this and flags concurrent-download limiting as `[open]`. Worth one explicit line that DB-level streaming is *not* achieved under bytea (only response-level), so the upstream "OOM 회피" claim is met by cap-plus-concurrency-limit rather than by true streaming — closes the loop with services.md/component-methods.
- **S3 — Image extension list inconsistency between artifacts.** business-rules.md BR-U6-2 lists images as `png/jpg/jpeg/gif/webp`; domain-entities.md Assumptions lists `png/jpg/gif/webp` (omits `jpeg`). jpg/jpeg share MIME `image/jpeg`, so this is a cosmetic extension-list drift, but align the two so the whitelist reads identically.
- **S4 — Participant predicate vs enrollment state.** BR-U6-3 uses "APPLIED 멘티" as the membership predicate. Posts/notices exist only in 진행중 (after ②시작), by which point enrollment is settled — so an APPLIED-only predicate may be too broad or too narrow depending on U4's terminal enrollment states. This is within U6's rule authority and correctly deferred to U4 contract alignment (`[open]`); a one-line note on which enrollment state(s) confer content access would sharpen it for the implementer.

Verdict: READY
