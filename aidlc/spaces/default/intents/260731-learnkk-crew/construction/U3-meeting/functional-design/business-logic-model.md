# Business Logic Model — U3 Meeting (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U3 Meeting(service, XL). 스토리 US-2.1a/2.1b/2.2/2.3/3.1/3.4/6.1/7.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U3), requirements.md(FR2.x·FR3.4·②=FR2.3/US-6.1·FR7.3), components.md(C2·MeetingService/MeetingApprovalService/SurveyTemplateService·FE meetings feature), component-methods.md(시그니처), services.md(오케스트레이션·③ read U5·운영허브 read U4/U8), U1(MeetingStatus/ErrorPayload/Principal). 상태머신 워크플로우·문항 빌더·운영 허브·FE 화면 정의. -->

## 개요

U3의 상태머신 전이 워크플로우, 문항 빌더, 운영 허브(read 조합), 목록/상세, FE 화면을 정의한다. 전이는 business-rules BR-U3-1 전이표를 절차로 표현.

## 상태 전이 워크플로우

### W1. 모임 개설 (US-2.1a/2.1b / createMeeting + upsertQuestions)

```
createMeeting(mentorId, {title, topic, weeks, recruitPeriod, capacity, format, initialContent}):
  1. Principal.role == MENTOR? 아니면 403
  2. 필드 검증(weeks>0, capacity>0, 기간 유효). 실패 400
  3. Meeting insert(status=PENDING_APPROVAL, mentorId)
  4. return MeetingResponse
upsertQuestions(mentorId, meetingId, SurveyQuestion[]):
  소유 멘토 확인(403), status가 IN_PROGRESS 이전인지(BR-U3-7, 아니면 409)
  → 문항 교체/저장
```

### W2. 관리자 전이 액션 (MeetingApprovalService)

각 액션 공통 전처리: `Principal.role == ADMIN?` 아니면 403. 대상 조회 없으면 404. 전이표(BR-U3-1)로 status 검증 → 조건부 UPDATE(`WHERE status=<expected>`), 0 rows면 409.

```
approveCreation(admin, meetingId):        # ① T1
  status==PENDING_APPROVAL → RECRUITING, else 409 MEETING_INVALID_TRANSITION
rejectCreation(admin, meetingId, reason):  # T2
  status==PENDING_APPROVAL → REJECTED(rejectReason=reason), else 409
confirmRecruitment(admin, meetingId, proceed):  # 모집확정 T3/T4
  status==RECRUITING? else 409
  # 신청 수는 관리자가 확정 화면에서 참고(U4 read, ADR-007 R-1) — 분기는 proceed 입력만 사용
  proceed==true  → READY_TO_START
  proceed==false → CANCELLED
approveStart(admin, meetingId):            # ② T5
  status==READY_TO_START → IN_PROGRESS, else 409
completeMeeting(admin, meetingId):         # ③ T6 (관리자 직접, rev-mk)
  status==IN_PROGRESS? else 409
  allSessionsEnded = U5.allScheduledSessionsEnded(meetingId)  # read (ADR-007 R-2)
  not allSessionsEnded → 409 MEETING_SESSIONS_NOT_ENDED
  → COMPLETED
```

- 멘티 수료 확정(④)은 이 Unit이 아님(U5). ③(모임 완료)과 ④(멘티 수료)는 독립.

### W3. 목록·조회 (US-3.1 / getMeeting / listRecruiting)

```
listRecruiting(filter): status==RECRUITING 모임 페이지 → MeetingSummary[]
getMeeting(meetingId): 존재 확인(404) → 권한별 상세(참여자/멘토/관리자)
```

### W4. 멘토 운영 허브 (US-2.3) — FE 화면 레벨 조합

백엔드 `listMyMeetings`는 U3 소유 데이터만 반환한다. 신청자·사전설문 응답은 **운영 허브 화면이 FE 단일 API client로 각 소유 Unit 엔드포인트를 호출해 조합**한다 — 백엔드 U3→U4/U8 의존을 만들지 않아 순환을 회피(U8 depends_on에 U3 존재).

```
# 백엔드 (U3 소유만)
listMyMeetings(mentorId):
  Principal.role==MENTOR 확인 → 자기 모임 목록(status 포함) → MeetingResponse[]

# FE 운영 허브 화면 (조합 — 백엔드 아님)
hubScreen(meetingId):
  meeting    = U3.getMeeting(meetingId)             # 소유
  applicants = U4.listApplicants(meetingId)         # U4 엔드포인트 호출(ADR-007 R-1 범위)
  preSurvey  = U8.getAnswers(meetingId, ...)        # U8 엔드포인트 호출(화면 조합 전용, ADR-007 밖)
  → 화면에서 조합 렌더 (각 호출은 소유 Unit이 403 권한 경계 집행)
```

- U4 read는 ADR-007 R-1(Meeting↔Enrollment)이 커버하는 교차참조. U8 read는 ADR-007 범위 밖이라 **화면 레벨 조합 전용**. 어느 경우도 U3 백엔드가 U4/U8을 직접 read하지 않는다.

## FE 화면 (meetings feature)

- **멘토 개설 화면:** 모임 기본정보 폼 + 사전설문 문항 빌더(문항 추가/삭제/순서·유형). 검증(weeks/capacity/기간). 개설 → PENDING_APPROVAL 안내.
- **모임 목록(멘티):** RECRUITING 모임 카드 목록(제목·주제·정원·모집기간·상태 뱃지). 상세 → 신청(U4 화면 연계).
- **멘토 운영 허브:** 자기 모임 목록 + 상태별 액션. 상세에 신청자 목록·사전설문 응답(read 조합). 상태 뱃지·다음 액션 안내.
- **관리자 액션:** 승인 큐(U9 화면)에서 ①/모집확정/②/③ 액션 호출 → 이 Service. (큐 조회는 U9, 액션은 U3.)
- 접근성(CC-2)·목록 상태(CC-3 로딩/빈/에러) 상속.

## 통합 지점 요약

- **백엔드 read-in(U3가 호출):** U5 세션 종료 확인만(③완료 서버측 전제, ADR-007 R-2 — 컨트롤러 오케스트레이션 또는 U5 read 포트, 시그니처는 U5 functional-design 확정). ADR-007은 Status: Proposed이며 U1은 아직 read 포트를 정의하지 않음 — 해소 메커니즘(컨트롤러 조합 또는 U1/U5 소유 read 포트)은 U5 functional-design에서 확정.
- **FE 화면 조합(백엔드 아님):** 운영 허브의 U4(신청자, ADR-007 R-1)·U8(사전설문 응답, ADR-007 밖) read는 화면이 각 Unit 엔드포인트를 호출해 조합. U3 백엔드는 U4/U8을 호출하지 않음 → U3↔U8/U3↔U4 백엔드 순환 없음.
- **write-out:** 없음(U3는 자기 meeting/survey_question만 write). 관리자 액션 큐 조회는 U9가 U3 read.
- 상태 전이는 U3 단일 집행(다른 Unit이 meeting.status를 직접 쓰지 않음).

## 에러·엣지 케이스

- 이중 승인(동시 ①): 조건부 UPDATE로 하나만 성공, 나머지 409.
- ③ 완료 시 세션 미종료: 409 `MEETING_SESSIONS_NOT_ENDED`.
- 모집 미달 + proceed=true: 허용(관리자 판단, READY_TO_START).
- 종료 상태 재액션: 409.
- ②후 문항 편집 시도: 409/400(BR-U3-7).

## Assumptions & Open Questions

- **[assumption]** 조건부 UPDATE로 전이 직렬화(락 상세 구현). 문항 없이 개설 허용.
- **[open]** U5 `allScheduledSessionsEnded`·U4 `count/listApplicants` read 포트 시그니처는 U5/U4 functional-design과 확정(계약 정합).
- READY_TO_START에서 신청 취소는 U4(②전 허용, US-3.3).
## Review

**Reviewer:** aidlc-architecture-reviewer-agent — re-review iteration 2 (adversarial functional-design review; Unit U3 Meeting, kind=service, XL; scope: business-logic-model.md + business-rules.md + domain-entities.md against the consumed inception contracts + U1 shared kernel)

Verdict: READY

이터레이션 1의 유일한 blocking(B1: U8 read가 잘못된 계약 ADR-007 R-1에 귀속되고 U3↔U8 백엔드 순환 위험 미해소)은 옵션 (a)로 완전히 해소되었다. 백엔드에는 U3→U8/U3→U4 read가 남아 있지 않고, U8 read는 FE 화면 조합 전용으로 명시되며 ADR-007 귀속이 제거되었다. 회귀 없음. 잔여 1건은 non-blocking(S5, domain-entities 꼬리 문장의 낡은 문구).

### B1 재검증 — RESOLVED

- **백엔드 U3→U8 read 제거 확인.** `business-logic-model.md` W4가 백엔드/FE를 명확히 분리한다: 백엔드 `listMyMeetings(mentorId)`는 "U3 소유 데이터만 반환"(`MeetingResponse[]`, component-methods C2 시그니처와 정합)하고, `preSurvey = U8.getAnswers(...)`·`applicants = U4.listApplicants(...)`는 `MeetingService` 본문 밖 **FE `hubScreen` 조합**(단일 API client가 각 소유 Unit 엔드포인트 호출)으로 이동했다. business-rules BR-U3-6도 "백엔드는 U3 모임 데이터만 반환 … 신청자·사전설문 응답은 운영 허브 화면이 FE에서 U4/U8 엔드포인트를 각각 호출해 조합"으로 일치. domain-entities 「관계·통합 지점」도 "백엔드 `MeetingService`는 U4/U8을 직접 호출하지 않는다"로 정합.
- **story-map 정합.** unit-of-work-story-map.md US-2.3 = "U3 **화면**이 U4·U8 read 조합" — 수정된 화면-레벨 조합과 일치.
- **잘못된 ADR-007 R-1 귀속 제거.** U8 read는 세 문서 본문에서 "ADR-007 범위 밖, 화면 조합 전용"으로 재분류되고, U4 read만 ADR-007 R-1(Meeting↔Enrollment)로 라벨. unit-of-work.md(ADR-007 = U3↔U4 R-1, U3↔U5 R-2; U8 부재)와 정합. U8 `depends_on=U1,U2,U3,U4`(U8→U3 간선 존재)에 대해 역방향 백엔드 read가 없으므로 **U3↔U8 백엔드 순환 없음**. U3 `depends_on=[U1,U2]` 유지와도 모순 없음.

### 회귀 검증 (이터레이션 1 PASS 항목 재확인 — 유지)

- **상태 전이표 정합 — PASS(유지).** BR-U3-1 T1–T6 불변. U1 `MeetingStatus` 값만 사용, FR2.3 흐름 전 커버, 종료 상태 재전이 금지, 그 외 409 `MEETING_INVALID_TRANSITION`. W2 절차·전이표·생명주기 다이어그램 3문서 일치.
- **승인 지점 모델링 — PASS(유지).** ①=T1/②=T5/③=T6(관리자 직접, BR-U3-5)/모집확정=T3·T4. ④는 U5. OQ1 [decided](모집확정=독립 운영 액션)·`recruitConfirm[]` 정합 유지.
- **③ 완료 전제(U5 세션 종료 read) — PASS(유지).** `completeMeeting`이 `U5.allScheduledSessionsEnded` read를 **ADR-007 R-2**로 유지하고 해소 메커니즘(컨트롤러 오케스트레이션 또는 U5 read 포트, 시그니처는 U5 functional-design 확정)을 명시. write(status=COMPLETED)는 U3 단일 소유. component-methods `completeMeeting` 노트(C4 read, ADR-007 R-2, 오케스트레이션 후 호출)와 정합 — 미해소 순환 주장 없음. B1과 달리 ADR-007 백킹이 실재하므로 blocking 아님(S1 참조).
- **C2 메서드 충실도 — PASS(유지).** 편집 후에도 `listMyMeetings -> MeetingResponse[]` 등 전 시그니처가 component-methods.md C2와 일치. U8 제거가 시그니처를 훼손하지 않음.
- **동시성 — PASS(유지).** `WHERE status=<expected>` 조건부 UPDATE(0 rows→409) 직렬화 유지.
- **스토리 커버리지 — PASS(유지).** US-2.1a/2.1b/2.2/2.3/3.1/3.4/6.1/7.3 8개 모두 유지·추적 가능. US-2.3은 백엔드+FE 조합으로 여전히 커버.
- **센서 — PASS.** required-sections: 세 문서 H2 ≥2(business-logic-model 7개 포함). upstream-coverage: business-logic-model 헤더/prose가 unit-of-work·story-map·requirements·components·component-methods·services 참조. TS/JS/TSX 스니펫 없음 → linter/type-check 대상 없음.

### 이전 Suggestion 처리 확인

- **S2 — 처리됨.** W2 `confirmRecruitment`가 `U4.count` read 라인을 제거하고 "신청 수는 관리자가 확정 화면에서 참고(U4 read, ADR-007 R-1) — 분기는 proceed 입력만 사용" 주석으로 대체. domain-entities도 "정원 대비 신청 수는 … 화면 표시·보조 용도(분기는 관리자 proceed)"로 명시.
- **S4 — 처리됨.** 세 문서 헤더가 ②를 "FR2.3/US-6.1"(또는 "FR2.3 상태흐름/US-6.1")로 정정. FR6.1(U5 세션 일정) 오인용 제거.
- **S1 — 부분 처리(non-blocking).** business-logic-model 통합 지점 요약이 "ADR-007 Status: Proposed, U1은 아직 read 포트 미정의 — 해소 메커니즘은 U5 functional-design 확정"으로 완화됨. 다만 아래 S5 참조.

### Suggestions (non-blocking)

- **S5 — domain-entities 꼬리 Assumptions 문장이 낡은 문구를 유지(정합성 nit).** `domain-entities.md` 마지막 「Assumptions & Open Questions」의 `[open] read 교차참조(U4/U5/U8)는 U1 계약 read 포트 경유 — 순환 회피(ADR-007)` 문장이 여전히 U8을 "U1 계약 read 포트/ADR-007"에 묶어, 같은 문서의 정정된 본문(「관계·통합 지점」: U8 = ADR-007 밖, FE 화면 조합 전용)과 모순된다. 권위 있는 본문·의사코드·BR-U3-6이 모두 정정돼 있어 개발자가 백엔드 U3→U8 read를 구현할 근거가 없으므로 **순환 위험 없음 → non-blocking**. 그 한 문장을 "U4는 ADR-007 R-1·U5는 R-2 read(컨트롤러/포트 해소), U8은 FE 화면 조합 전용(ADR-007 밖)"으로 정정 권장.
- **S3(이월) — `U4.listApplicants`·`U5.allScheduledSessionsEnded` read 포트 시그니처는 component-methods에 미명명.** 본 문서가 [open](U4/U5 functional-design 확정)으로 이월했으므로 수용 가능. 계약 정합 확인 항목으로 추적 유지 권장.

Verdict: READY
