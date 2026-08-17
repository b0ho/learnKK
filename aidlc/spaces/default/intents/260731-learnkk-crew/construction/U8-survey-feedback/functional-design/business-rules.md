# Business Rules — U8 Survey/Feedback (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U8 Survey/Feedback(service). 스토리 US-3.6/8.1/8.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U8·사전설문 ②후·문항 틀 U3 read), requirements.md(FR3.6 ②후 응답·FR8.1 제출·FR8.2 열람), components.md(C7), component-methods.md(PreSurveyService/FeedbackService), services.md(REST/에러 경계·오케스트레이션), U1 business-rules(CC-1·인가). -->

## 개요

U8은 사전설문 응답(②후 게이팅)·과정 설문·피드백 열람 규칙을 소유한다. 문항 틀은 U3, 응답은 U8. U1 CC-1 상속.

## BR-U8-1. 사전설문 응답 게이팅 (US-3.6, FR3.6)

- **응답 제출(submitAnswers)**은 **모임 status=IN_PROGRESS(②시작 이후)에만**(FR3.6 rev-us). ② 전 제출 시도 → 409 `PRESURVEY_NOT_OPEN`(또는 비노출). 제출 마감은 COMPLETED 시점(그 이후 제출 불가) [assumption].
- **응답 조회(getAnswers)**는 status 게이팅 없이 **인가 기준만** — 소유 멘토·관리자·본인(BR-U8-2). 모임이 COMPLETED 되어도 멘토·관리자는 계속 열람 가능해야 하므로(FR3.6 "멘토·관리자 열람"·U3 운영허브·U9 모니터링) 조회에 IN_PROGRESS-only 게이트를 두지 않는다.
- 응답자는 참여 멘티 본인(U4 APPLIED read, Principal). 비참여자 403.
- 문항은 U3 `getQuestions(meetingId)` read. 응답은 각 questionId에 매핑, `unique(question,mentee)`(재제출 갱신 [assumption]).
- 필수 문항 미응답 처리(OQ7) [assumption]: 필수 문항 누락 시 400 또는 부분 저장 허용 — 파일럿은 필수 문항 미응답 400.

## BR-U8-2. 사전설문 응답 열람 (US-2.3 read / US-3.6)

- `getAnswers(requester, meetingId, menteeId)`: 소유 멘토(자기 모임)·관리자만 열람(403 경계). 멘티는 본인 응답만.
- U3 운영 허브 화면이 이 응답을 read 조합(FE 조합).

## BR-U8-3. 과정 설문·피드백 제출 (US-8.1, FR8.1)

- 제출자는 참여 멘티(U4 read). 제출 시점 [assumption]: 진행중(IN_PROGRESS)·완료(COMPLETED) 모임의 참여 멘티. 그 외 409.
- 본인만 제출(menteeId==Principal). 재제출 정책 [assumption].

## BR-U8-4. 피드백 열람 권한 (US-8.2, FR8.2)

- `listFeedback(requesterId, meetingId)`는 **소유 멘토(자기 모임)·관리자만** 열람 → **타 모임 멘토 403**(component-methods·FR8.2 명시, 멘티 열람 경로 없음). 관리자는 전 모임 열람.
- 멘티는 피드백 **제출**만(본인). listFeedback는 meetingId 단위 반환이며 menteeId 파라미터가 없어 "본인 것만" 조회를 표현할 수 없다 — 멘티 본인 열람은 스코프 밖(FR8.2는 멘토+관리자 열람만).

## BR-U8-5. 인가 요약

- 사전설문 응답 제출: 참여 멘티 본인(②후). 응답 열람(getAnswers, menteeId 인자 있음): 소유 멘토·관리자·본인(status 게이팅 없음).
- 피드백 제출: 참여 멘티 본인. 피드백 열람(listFeedback, meetingId 단위): 소유 멘토·관리자만(멘티 본인 열람 경로 없음, FR8.2).
- 위반 403.

## 에러 처리 (U1 CC-1 상속)

- ②전 응답 409 `PRESURVEY_NOT_OPEN`, 필수 미응답 400, 인가 403, 미존재 404. ErrorPayload·한국어.

## Assumptions & Open Questions

- **[decided]** 사전설문 응답 ②후만(FR3.6). 피드백 열람 타 모임 멘토 403.
- **[assumption]** 필수 문항 미응답 400(OQ7), 재제출 갱신, 과정 설문 제출 시점(진행/완료), 문항당 1응답.
- **[open]** 과정 설문 문항 구조, U3 상태·문항 read·U4 참여자 read 포트 시그니처.
