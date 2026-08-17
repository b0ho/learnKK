# Business Rules — U3 Meeting (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U3 Meeting(service, XL). 스토리 US-2.1a/2.1b/2.2/2.3/3.1/3.4/6.1/7.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U3 상태머신 단일 소유), requirements.md(FR2.1~2.3·FR3.4·②=FR2.3/US-6.1·FR7.2/7.3·rev-mk 관리자 직접 ③), components.md(C2), component-methods.md(MeetingApprovalService 4지점+모집확정), services.md(③ read U5), U1 business-rules(CC-1·MeetingStatus 값·인가). U3는 U1 MeetingStatus enum을 상속하고 전이 규칙을 단일 집행. -->

## 개요

U3는 모임 상태머신의 **단일 집행자**(unit-of-work.md, ADR-006). U1이 정의한 MeetingStatus 값 위에서 전이표·전제조건·인가를 확정한다. 불법 전이는 항상 409(U1 BR-U1-1/BR-U1-3).

## BR-U3-1. 상태 전이표 (정본)

| # | 전이 | 트리거(액션·역할) | 전제조건 | 실패 |
|---|------|-------------------|----------|------|
| T1 | PENDING_APPROVAL → RECRUITING | ① `approveCreation`(ADMIN) | status=PENDING_APPROVAL | 그 외 status → 409 |
| T2 | PENDING_APPROVAL → REJECTED | `rejectCreation`(ADMIN, reason) | status=PENDING_APPROVAL | 409 |
| T3 | RECRUITING → READY_TO_START | 모집확정 `confirmRecruitment(proceed=true)`(ADMIN) | status=RECRUITING | 409 |
| T4 | RECRUITING → CANCELLED | 모집확정 `confirmRecruitment(proceed=false)`(ADMIN, 미달 시) | status=RECRUITING | 409 |
| T5 | READY_TO_START → IN_PROGRESS | ② `approveStart`(ADMIN) | status=READY_TO_START | 409 |
| T6 | IN_PROGRESS → COMPLETED | ③ `completeMeeting`(ADMIN, 직접) | status=IN_PROGRESS **AND 전 세션 종료**(U5 read) | 미완료/중복 → 409 |

- 위 6개 외 전이는 모두 불법 → **409 `MEETING_INVALID_TRANSITION`**.
- 종료 상태(REJECTED/CANCELLED/COMPLETED)에서 어떤 전이도 불가(409).
- 동시 전이 경합(같은 모임 중복 승인): 낙관적 락 또는 `WHERE status=<expected>` 조건부 UPDATE로 직렬화, 실패 시 409.

## BR-U3-2. 승인 지점·모집확정의 관계 (OQ1/A2 해소)

- 4지점 승인 = ①개설(T1) / ②시작(T5) / ③모임완료(T6) / ④멘티수료(U5 소관). U3는 ①②③을 집행, ④는 U5.
- **모집확정(T3/T4)은 4지점과 별개의 독립 운영 액션**(OQ1 해소). 정식 5번째 승인으로 편입하지 않고, 관리자 운영 액션으로 둔다. U9 승인 큐는 이를 `recruitConfirm[]` 별도 항목으로 노출(component-methods AdminQueryService와 정합).
- 근거: ①②③④는 품질 게이트(승인/판정) 성격, 모집확정은 모집 마감 후 진행/취소 결정 성격 — 의미가 달라 분리.

## BR-U3-3. 개설 규칙 (US-2.1a/2.1b)

- 개설자는 role=MENTOR(Principal, U2). 아니면 403.
- 필수: title, topic, weeks(>0), 모집기간, capacity(>0), format. 위반 400.
- 사전설문 문항(SurveyQuestion)은 개설/수정 중 자유 구성(FR2.1). 문항 없이 개설도 허용 [assumption].
- 개설 직후 status=PENDING_APPROVAL(①대기).

## BR-U3-4. 모집확정 규칙 (US-3.4, T3/T4)

- RECRUITING 상태에서 관리자가 모집 마감 후 확정.
- 정원 대비 신청 수는 U4 read(신청자 수). 미달 시 관리자가 `proceed` 여부 결정:
  - proceed=true → READY_TO_START(진행).
  - proceed=false → CANCELLED(취소, 종료).
- 정원 충족이어도 관리자가 확정 액션을 해야 시작대기로 전환(자동 아님).

## BR-U3-5. ③ 모임 완료 규칙 (US-7.3, rev-mk — 관리자 직접)

- **멘토의 완료 인정 신청 단계 없음.** 전 세션 종료 시 관리자가 직접 `completeMeeting` 호출(FR7.2).
- 전제조건 "전 세션 종료": U5 `SessionService` read로 확인(모임의 모든 예정 세션이 종료 시각 경과, ADR-007 R-2). 미충족 시 409 `MEETING_SESSIONS_NOT_ENDED`.
- 쓰기(status=COMPLETED)는 U3 단일 소유. 판정 로직 자체(세션 종료 확인)는 U5 read 조합.
- 멘티 수료 확정(④)은 U3 완료와 독립 — U5 소관.

## BR-U3-6. 목록·조회·운영 허브 (US-3.1/2.3)

- `listRecruiting`: status=RECRUITING 모임만 멘티에게 노출(US-3.1). 페이지네이션(U1 규약).
- `listMyMeetings`(멘토 운영 허브): 자기 모임만(403 경계) — 백엔드는 U3 모임 데이터만 반환. 신청자 목록·사전설문 응답은 **운영 허브 화면이 FE에서 U4/U8 엔드포인트를 각각 호출해 조합**(백엔드 U3→U4/U8 의존 없음, 순환 회피). U4 read는 ADR-007 R-1(Meeting↔Enrollment) 범위, U8 read는 화면 조합 전용(ADR-007 범위 밖).
- 상세 조회: 참여자·멘토·관리자 권한 경계. 비공개 정보(신청자 등)는 소유 멘토/관리자만.

## BR-U3-7. 문항 편집 제약

- 사전설문 응답 수집이 ②시작 이후 시작(U8, US-3.6)되므로, **status=IN_PROGRESS 이후 문항 틀(SurveyQuestion) 편집 금지** [assumption](응답 정합성). 위반 409/400.

## 에러 처리 (U1 CC-1 상속)

- 불법 전이/중복/전제 미충족 409, 인가 위반 403, 검증 400, 미존재 404. ErrorPayload, 한국어 message.

## Assumptions & Open Questions

- **[assumption]** 문항 없이 개설 허용, ②후 문항 편집 금지, 반려 사유 필수.
- **[decided]** 모집확정=독립 운영 액션(OQ1). ③=관리자 직접(rev-mk).
- **[open]** 낙관적 락 vs 조건부 UPDATE 상세는 구현. READY_TO_START에서의 신청 취소는 U4(②전 허용).
- 전이 전제(전 세션 종료)는 U5 계약 read에 의존 — U5 functional-design과 정합 필요.
