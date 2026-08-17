# Domain Entities — U3 Meeting (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U3 Meeting(kind=service, XL). 스토리: US-2.1a/2.1b/2.2/2.3/3.1/3.4/6.1/7.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U3=C2 상태머신 소유·문항 빌더·운영 허브), requirements.md(FR2.1~2.3·FR3.1/3.4·②=FR2.3 상태흐름/US-6.1·FR7.2/7.3(③)), components.md(C2·소유 데이터 meeting/survey_question), component-methods.md(MeetingService/MeetingApprovalService/SurveyTemplateService), services.md(오케스트레이션·③ read U5), U1 domain-entities(MeetingStatus enum·Role·ErrorPayload). Entity는 API 비노출(NFR8). -->

## 개요

U3는 C2(모임) 도메인의 엔티티를 소유한다: `meeting`(상태머신 대상)과 `survey_question`(사전설문 문항 틀). MeetingStatus enum은 U1 소유(정의), 전이 집행은 U3(business-rules). 사전설문 **응답**은 U8, 문항 **틀**만 U3.

## 엔티티

### Meeting (모임)

US-2.1a 개설, 상태머신의 핵심 애그리게이트.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | U1 baseline |
| `mentorId` | BIGINT (FK→user) | NOT NULL | 개설 멘토(role=MENTOR) |
| `title` | varchar | NOT NULL | 모임명 |
| `topic` | varchar | NOT NULL | 주제(FR2.1) |
| `weeks` | int | NOT NULL, >0 | 학습 주차 수(FR2.1) |
| `recruitStart`/`recruitEnd` | date/timestamptz | NOT NULL | 모집기간(FR2.1) |
| `capacity` | int | NOT NULL, >0 | 정원(FR2.1) — 신청 정원 판정은 U4 read |
| `format` | varchar | | 진행방식(온/오프 등, FR2.1) |
| `initialContent` | text | 선택 | 개설 시 학습자료 소개(FR2.1) — 주차 게시글은 U6 |
| `status` | varchar(enum MeetingStatus) | NOT NULL | U1 MeetingStatus, 기본 PENDING_APPROVAL |
| `rejectReason` | varchar | nullable | 반려 시 사유(US-2.2) |
| `createdAt`/`updatedAt` | timestamptz | | baseline |

- **상태 기본값:** 개설 시 `PENDING_APPROVAL`(개설신청).
- **불변식:** `status`는 U1 MeetingStatus 값만. 전이는 business-rules 전이표만 허용(그 외 409).
- 공지·주차 게시글·세션은 각각 U6·U5 소유(여기 아님). Meeting은 그 앵커(FK 대상).

### SurveyQuestion (사전설문 문항 틀)

US-2.1b 문항 빌더. 멘토가 개설/수정 중 자유 구성(FR2.1). 응답(survey_answer)은 U8.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | 소속 모임 |
| `orderNo` | int | NOT NULL | 표시 순서 |
| `text` | varchar | NOT NULL | 문항 내용 |
| `type` | varchar(enum) | NOT NULL | 문항 유형(단답/장문/선택 등) [assumption] |
| `options` | text[]/json | nullable | 선택형 보기 |
| `required` | boolean | 기본 true | 필수 여부 |

- 문항은 모임에 종속. 모임이 진행중(②) 이후 문항 틀 변경 제약 [assumption]: 응답 수집 시작(②후) 이후 문항 편집 금지(응답 정합성) — business-rules.

## 상태 생명주기 (MeetingStatus, business-rules 전이표 요약)

```
PENDING_APPROVAL(개설신청)
  ─①승인→ RECRUITING(모집중)
  ─①반려→ REJECTED(반려, 종료)
RECRUITING
  ─모집확정(proceed)→ READY_TO_START(시작대기)
  ─모집확정(취소/미달)→ CANCELLED(취소, 종료)
READY_TO_START
  ─②시작→ IN_PROGRESS(진행중)
IN_PROGRESS
  ─③완료(전 세션 종료 전제, 관리자 직접)→ COMPLETED(완료, 종료)
```

- 종료 상태(REJECTED/CANCELLED/COMPLETED)에서 재전이 없음(U1 BR-U1-3).

## 관계·통합 지점 (읽기 교차참조)

- `mentorId` → user(U2). 개설자 권한(MENTOR) 검증은 Principal(U2).
- **US-2.3 운영 허브(FE/화면 레벨 조합):** 멘토 운영 허브 **화면**이 U3 모임 데이터 + 신청자(U4 `listApplicants`) + 사전설문 응답(U8 `getAnswers`)을 **FE 단일 API client로 각 소유 Unit 엔드포인트를 호출해 조합**한다(story-map "U3 화면이 U4/U8 read 조합"). 백엔드 `MeetingService`는 U4/U8을 직접 호출하지 않는다 — U3→U8 백엔드 의존을 만들지 않아 순환을 회피(U8 depends_on에 U3가 있으므로 역방향 백엔드 read는 금지).
  - U4(신청자·정원) read는 ADR-007 **R-1**(Meeting↔Enrollment)이 커버하는 교차참조로, 컨트롤러/화면 조합 또는 U4가 노출하는 read 엔드포인트로 해소(시그니처는 U4 functional-design 확정). U8(사전설문 응답)은 ADR-007 범위 밖이므로 **화면 레벨 조합만** 사용.
- **US-7.3 ③완료 전제:** "전 세션 종료"는 U5 `SessionService` read로 확인(ADR-007 **R-2**, Meeting↔Session). 이 read는 완료 액션의 서버측 전제 검증이라 컨트롤러 오케스트레이션 또는 U5 read 포트로 해소(시그니처는 U5 functional-design 확정). U3는 세션을 소유하지 않음.
- 정원(capacity) 대비 신청 수는 모집확정 판단의 **화면 표시·보조** 용도로 U4 read(분기 자체는 관리자 `proceed` 입력).

## Assumptions & Open Questions

- **[assumption]** 문항 유형 집합(단답/장문/선택), ②후 문항 편집 금지, 반려 사유 필수 여부.
- **[decided/OQ1]** 모집확정은 4지점 승인(①②③④)과 별개의 **독립 운영 액션**으로 모델(business-rules BR-U3 참조) — component-methods AdminQueryService가 `recruitConfirm[]`를 별도 큐로 반환하는 것과 정합.
- **[open]** 시작대기(READY_TO_START)에서 신청 취소 허용 여부는 U4 소관(US-3.3, ②전 취소 허용).
- read 교차참조: U4는 ADR-007 R-1·U5는 R-2 read(컨트롤러 오케스트레이션 또는 read 포트로 해소, 시그니처는 U4/U5 functional-design), U8은 ADR-007 범위 밖으로 **FE 화면 조합 전용**(백엔드 U3→U8 없음 → 순환 회피).
