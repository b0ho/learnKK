# Code Generation — Observation Diary (Bolt 7 Survey/Feedback)

<!-- 오케스트레이터 유지. 손으로 편집하지 않음. -->

## Interpretations
- 2026-08-23T15:45:00Z — Bolt 7(Survey/Feedback, U8) 대상. Bolt 1~3 패턴 동일: memory_path {unit-name}을 bolt 레벨(`construction/bolt7-survey/`)로 해석. 설계는 `260731-learnkk-crew/construction/U8-survey-feedback/`에서 상속. Brownfield — 신규 survey 모듈 + 시임 배선.
- 2026-08-23T15:45:00Z — 범위(bolt-plan Bolt 7): 사전설문 응답(②후 게이팅)·과정설문·멘토/관리자 피드백 열람. U8 owns survey_answer/feedback; 문항 틀(survey_question)은 U3 소유(read).

## Deviations
- (실행 중 기록)

## Tradeoffs
- 2026-08-23T15:45:00Z — [gotcha] SurveyQuestionDto에 `id` 미노출 → survey_answer.question_id FK 참조 불가. 결정: SurveyQuestionDto·openapi 스키마에 `id`(nullable, read 시 채움/upsert write 시 무시) 추가 — 최소 침습(Bolt 1/2 upsert 무영향). FE는 questionId로 응답 제출.
- 2026-08-23T15:45:00Z — [cross-module] U8 서비스는 EnrollmentService 선례대로 MeetingService(상태·mentorId)·SurveyTemplateService(getQuestions)·EnrollmentService(참여자 확인) 주입. 타 모듈 테이블 직접 접근 안 함. EnrollmentService에 `isActiveParticipant(meetingId,menteeId)` 추가.
- 2026-08-23T15:45:00Z — [gating] submitAnswers만 IN_PROGRESS 게이팅(BR-U8-1), getAnswers는 인가만(status 무관 — COMPLETED 후에도 멘토/관리자 열람). 피드백 제출=IN_PROGRESS/COMPLETED 참여멘티, 열람=소유멘토/관리자만(멘티 열람 경로 없음).

## Open questions
- 2026-08-23T15:45:00Z — [env] 통합 테스트 Testcontainers 미가용(Windows/Rancher JNA) → 라이브 API/UI E2E로 보완. ci-pipeline·operation은 project.md Scope Override로 build-and-test 후 SKIP.
- 2026-08-23T15:45:00Z — [assumption] 과정설문 문항 구조는 자유 서술(content text) — 설계 [open]. 재제출 갱신(upsert). 필수 미응답 400.
