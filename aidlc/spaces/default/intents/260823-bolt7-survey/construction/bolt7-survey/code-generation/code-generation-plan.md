# Code Generation Plan — Bolt 7 Survey/Feedback (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 7 = U8 Survey/Feedback. Brownfield: 신규 survey 모듈 + 시임 배선. 상속 설계: 260731-learnkk-crew intent U8 functional-design·nfr-requirements. 규칙: team.md·project.md·construction.md. -->

## 목표 (Definition of Done — bolt-plan Bolt 7)

> 사전설문 응답(②시작 이후 게이팅) · 과정설문 제출 · 멘토/관리자 피드백 열람.

- **사전설문 응답 제출**은 모임 IN_PROGRESS(②후)에만 → 그 외 409 `PRESURVEY_NOT_OPEN`. 필수 문항 누락 400.
- **응답 열람**(getAnswers)은 인가 기준만(소유 멘토·관리자·본인) — status 게이팅 없음(COMPLETED 후에도 멘토/관리자 열람).
- **피드백 제출**=참여 멘티 본인(IN_PROGRESS/COMPLETED), **피드백 열람**=소유 멘토·관리자만(타 모임 멘토 403, 멘티 열람 경로 없음).
- 확신 가설(bolt-plan): 사전설문이 ②시작 이후에만 열리고, 피드백 열람 권한 경계가 지켜짐.
- test-alongside, BE/FE 각 80% floor + 게이팅·인가 경계 시나리오.

## 상속·통합 지점 (기존 코드)

- **U8→U3 read**: `MeetingService.getMeeting`(status·mentorId), `SurveyTemplateService.getQuestions`(문항 틀·required). U3 테이블 직접 접근 금지.
- **U8→U4 read**: 참여자 확인 — `EnrollmentService.isActiveParticipant(meetingId, menteeId)` 신규 추가(APPLIED).
- **재사용**: kernel 에러 계층, Principal(isMentee/isMentor/isAdmin), PageResponse, SessionAuthInterceptor. 크로스모듈 주입은 EnrollmentService 선례.
- **시임 배선**: MyLearningPage `mentor-hub-note`(사전설문 응답 U8/Bolt7 placeholder) → 멘토 피드백/응답 열람 링크. 멘티 신청 카드에 IN_PROGRESS 시 "사전설문 응답" 액션 추가.
- **gotcha**: SurveyQuestionDto에 `id` 추가(read 채움/write 무시) + openapi 스키마 반영(계약 테스트).

## 범위 밖 (이월)
- U5 세션 일정 조합(Bolt 6), U9 모니터링(Bolt 8). 과정설문 고정 문항 구조(자유 서술 content로 구현).

---

## 실행 단계 (layer-by-layer)

### Step 1: DB 스키마 — V5 마이그레이션
- [x] `V5__survey_feedback.sql` — `survey_answer`(id, meeting_id FK, question_id FK→survey_questions ON DELETE CASCADE, mentee_id FK, answer_text text, created_at; **UNIQUE(question_id, mentee_id)**), `feedback`(id, meeting_id FK, mentee_id FK, content text NOT NULL, created_at; **UNIQUE(meeting_id, mentee_id)**). 인덱스(meeting_id).
- 추적: domain-entities, BR-U8-1(unique), V1~V4 규약

### Step 2: U3 문항 id 노출 (gotcha 해소)
- [x] `meeting/dto/SurveyQuestionDto.java`에 `Long id` 추가(record, `from`에서 채움). upsert 경로는 id 무시(신규 저장) — 기존 동작 보존.
- [x] `contracts/openapi.yaml` SurveyQuestionDto 스키마에 `id` 추가(계약 테스트 정합).
- 추적: gotcha, OpenApiContractTest

### Step 3: U4 참여자 확인 포트
- [x] `enrollment/service/EnrollmentService.java`에 `boolean isActiveParticipant(Long meetingId, Long menteeId)` 추가(APPLIED 존재). (또는 repository `existsByMeetingIdAndMenteeIdAndStatus`.)
- 추적: BR-U8-1/3 참여자 판정, EnrollmentService 선례

### Step 4: 도메인 — Entity + Repository (신규 survey 모듈)
- [x] `survey/entity/SurveyAnswer.java`, `survey/entity/Feedback.java`(FK by id, 크로스모듈 ORM 연관 없음).
- [x] `survey/repository/SurveyAnswerRepository.java`(findByMeetingIdAndMenteeId, findByQuestionIdAndMenteeId, findByMeetingId), `FeedbackRepository.java`(findByMeetingId, findByMeetingIdAndMenteeId).
- 추적: domain-entities C7

### Step 5: kernel — 에러 코드
- [x] `ErrorCodes.java` `// --- Survey / feedback domain ---`: PRESURVEY_NOT_OPEN, PRESURVEY_REQUIRED_MISSING, PRESURVEY_FORBIDDEN, PRESURVEY_NOT_FOUND, FEEDBACK_FORBIDDEN, FEEDBACK_NOT_OPEN.
- 추적: CC-1 매핑

### Step 6: 서비스 (C7)
- [x] `survey/service/PreSurveyService.java`(MeetingService+SurveyTemplateService+EnrollmentService 주입):
  - `submitAnswers(Principal, meetingId, List<AnswerItem>)` — 참여자 403(isActiveParticipant), status==IN_PROGRESS else 409 PRESURVEY_NOT_OPEN, getQuestions로 필수 검증(누락 400), question별 upsert(unique question,mentee).
  - `getAnswers(Principal, meetingId, menteeId)` — 소유 멘토(mentorId==userId)/ADMIN/본인(menteeId==userId) else 403. status 게이팅 없음.
- [x] `survey/service/FeedbackService.java`:
  - `submitFeedback(Principal, meetingId, content)` — 참여자 본인 + status∈{IN_PROGRESS,COMPLETED} else 403/409, upsert(unique meeting,mentee).
  - `listFeedback(Principal, meetingId)` — 소유 멘토/ADMIN only else 403(타 모임 멘토 403, 멘티 경로 없음).
- 추적: W1~W4, BR-U8-1~5

### Step 7: DTO + Controller + 인터셉터
- [x] DTO: `SurveyAnswerRequest`(List<AnswerItem{questionId, answerText}>), `SurveyAnswerResponse`(questionId, answerText), `FeedbackRequest`(content), `FeedbackResponse`(id, menteeId, content, createdAt).
- [x] `survey/web/SurveyController.java`: `POST /api/meetings/{id}/survey-answers`(멘티 제출), `GET /api/meetings/{id}/survey-answers/mine`(본인), `GET /api/meetings/{id}/mentees/{menteeId}/survey-answers`(소유 멘토/관리자). `FeedbackController.java`: `POST /api/meetings/{id}/feedback`(멘티 제출), `GET /api/meetings/{id}/feedback`(소유 멘토/관리자 목록).
- [x] `SessionAuthInterceptor.isProtected` 확장 + Pattern 상수(위 라우트 전부 보호).
- 추적: component-methods, 계약, 인터셉터

### Step 8: 백엔드 테스트 (Standard)
- [x] `PreSurveyServiceTest`: submit 정상·②전(NOT_OPEN)409·비참여자403·필수누락400·재제출 갱신; getAnswers 소유멘토/관리자/본인 200·타인403·COMPLETED 후 열람 가능.
- [x] `FeedbackServiceTest`: submit 정상·비참여자403·상태 위반; listFeedback 소유멘토/관리자200·타모임멘토403·멘티403.
- [x] `SurveyControllerTest`/`FeedbackControllerTest`(@WebMvcTest, @MockBean AuthService): 라우트·인가.
- [x] `integration/SurveyIntegrationTest`: 개설→①→모집확정→②시작→응답 제출→멘토 열람; 피드백 제출→멘토 열람·타모임멘토403 end-to-end.
- 추적: team.md Testing Posture

### Step 9: 계약 #1 — openapi.yaml
- [x] version bump(`0.3.0-bolt3`→`0.4.0-bolt7`), tags `survey`,`feedback` 추가. paths 5개, 스키마(SurveyAnswerRequest/Response·FeedbackRequest/Response, SurveyQuestionDto.id). `OpenApiContractTest` 신규 DTO assertConforms 추가.

### Step 10: Frontend API + 타입
- [x] `api/survey.ts`(submitAnswers, getMyAnswers, getMenteeAnswers), `api/feedback.ts`(submit, list). `api/types.ts`(SurveyAnswer*, Feedback*, SurveyQuestionDto.id). `api/index.ts` re-export.

### Step 11: Frontend 멘티 화면 (survey feature)
- [x] `features/survey/PreSurveyAnswerPage.tsx` — getQuestions 렌더 → 응답 폼(필수 표시, 선택형 options). ②전 진입 시 "시작 후 응답 가능" 안내. 제출 409/400 매핑.
- [x] `features/survey/FeedbackPage.tsx` — 과정설문 제출 폼(진행/완료 모임).
- [x] routes/paths + AppRouter 라우트(멘티 접근). MyLearningPage 멘티 신청 카드: IN_PROGRESS 시 "사전설문 응답"·"피드백" 액션 추가.
- 추적: FE 화면, CC-2/CC-3

### Step 12: Frontend 멘토/관리자 열람 (feedback view)
- [x] `features/survey/FeedbackViewPage.tsx` — 소유 멘토/관리자 피드백 목록 + 사전설문 응답 열람(getMenteeAnswers). `<RequireRole allow={['MENTOR','ADMIN']}/>`. MentorHub `mentor-hub-note` placeholder → 각 모임 열람 링크 배선.
- 추적: W2/W4, US-2.3

### Step 13: Frontend 테스트
- [x] PreSurveyAnswerPage(②전 안내·제출·409/400), FeedbackPage 제출, FeedbackViewPage(목록·권한), survey/feedback api 단위.

### Step 14: 문서
- [x] README에 Bolt 7 범위·엔드포인트 반영.

---

## Assumptions
- 사전설문 응답 제출만 IN_PROGRESS 게이팅, 조회는 인가만. 피드백 열람=소유멘토/관리자만.
- SurveyQuestionDto에 id 추가(additive, upsert 무영향). 참여자 확인은 EnrollmentService.isActiveParticipant.
- 과정설문 content=자유 서술(text). 재제출 갱신(upsert), 문항당 1응답.
- ci-pipeline·operation은 project.md Scope Override로 build-and-test 이후 SKIP.

## 테스트 전략 (Standard)
- 컴포넌트당 5~8 단위 + 게이팅/인가 경계 통합. 이 환경 Testcontainers 미가용 시 라이브 API/UI E2E 보완.
