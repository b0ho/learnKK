# Tech Stack Decisions — U8 Survey/Feedback (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U8 Survey/Feedback(service). 출처: business-logic-model.md(응답·피드백), business-rules.md(BR-U8-1 게이팅), requirements.md(C1·FR3.6/8.x), U1 tech-stack 상속. U8은 응답 저장·검증 기술 선택. -->

## 개요

U1 스택·계약 도구 상속. U8은 응답·피드백 저장 형태를 확정. 경량이라 신규 결정 최소.

## U8 기술 선택

### TD-U8-1. 응답 저장 — 정규화 survey_answer

- **결정:** `survey_answer(meetingId, questionId, menteeId, answerText)` + `unique(question,mentee)`. 문항 틀은 U3 소유(read).
- **근거:** 문항별 응답 질의·집계 용이. 재제출은 upsert.

### TD-U8-2. 과정 설문 저장 — feedback

- **결정:** `feedback(meetingId, menteeId, content)`. content 구조는 자유서술/고정셋 [assumption] — text 또는 json.
- **근거:** 과정 설문 문항 구조 미확정([open])이라 유연 저장.

### TD-U8-3. ②후 게이팅 — 모임 상태 read

- **결정:** 사전설문 제출 시 U3 모임 status(IN_PROGRESS) read로 게이팅(TD-U5류 스케줄러리스와 동일 요청 시점 판정). 조회는 게이팅 없음.

## 범위 밖

- 설문 분석·집계 리포트(FR9.2 TBD), 외부 설문 도구. CI/CD·운영(C3).

## Assumptions & Open Questions

- **[assumption]** 응답 정규화·upsert, feedback content 형태(text/json).
- **[open]** 과정 설문 문항 구조, U3 문항/상태 read·U4 참여자 read 포트 시그니처.
