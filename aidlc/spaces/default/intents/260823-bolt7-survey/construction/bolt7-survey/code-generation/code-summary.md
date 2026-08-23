# Code Summary — Bolt 7 Survey/Feedback (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 7 = U8. Brownfield: 신규 survey 모듈 + 시임 배선. git 브랜치 `bolt7`. -->

## 목표 달성
사전설문 응답(②시작 이후 게이팅)·과정설문 제출·멘토/관리자 피드백 열람. 문항 틀은 U3 read, 응답/피드백은 U8 소유.

## 생성 파일 (신규 `com.learnkk.survey` 모듈)
- `db/migration/V5__survey_feedback.sql` — survey_answer(UNIQUE(question_id,mentee_id)) + feedback(UNIQUE(meeting_id,mentee_id)), FK ON DELETE CASCADE, 인덱스.
- `survey/entity/{SurveyAnswer,Feedback}.java`(FK by id, 크로스모듈 ORM 없음), `survey/repository/{SurveyAnswerRepository,FeedbackRepository}.java`.
- `survey/service/PreSurveyService.java`(submitAnswers/getAnswers), `survey/service/FeedbackService.java`(submitFeedback/listFeedback) — MeetingService+SurveyTemplateService+EnrollmentService 주입.
- `survey/dto/{SurveyAnswerRequest(+AnswerItem),SurveyAnswerResponse,FeedbackRequest,FeedbackResponse}.java`, `survey/web/{SurveyController,FeedbackController}.java`(5 라우트).
- FE: `api/survey.ts`·`api/feedback.ts`, `features/survey/{PreSurveyAnswerPage,FeedbackPage,FeedbackViewPage}.tsx`.

## 수정 파일
- `meeting/dto/SurveyQuestionDto.java`(+`Long id`, additive — from()에서 채움, upsert 무시). openapi 스키마 반영.
- `enrollment/service/EnrollmentService.java`+`EnrollmentRepository.java`(+`isActiveParticipant`/`existsByMeetingIdAndMenteeIdAndStatus`).
- `kernel/error/ErrorCodes.java`(+6: PRESURVEY_NOT_OPEN/REQUIRED_MISSING/FORBIDDEN/NOT_FOUND, FEEDBACK_FORBIDDEN/NOT_OPEN).
- `auth/web/SessionAuthInterceptor.java`(신규 5 라우트 보호, GET questions 공개 유지).
- `contracts/openapi.yaml`(`0.4.0-bolt7`, survey+feedback tag, 5 paths, 스키마).
- FE: `api/{types,index}.ts`, `routes/{paths,AppRouter}.tsx`(feedbackView=RequireRole MENTOR/ADMIN), `MyLearningPage.tsx`(멘티 IN_PROGRESS 카드 "사전설문 응답"·"피드백" 액션 + MentorHub 열람 링크 배선).

## 주요 구현 결정
- **게이팅**: submitAnswers만 IN_PROGRESS(else 409 PRESURVEY_NOT_OPEN)·참여자(403)·필수 검증(400). getAnswers는 인가만(소유멘토/관리자/본인) — status 무관(COMPLETED 후에도 멘토/관리자 열람).
- **피드백**: 제출=참여멘티(IN_PROGRESS/COMPLETED), 열람=소유멘토/관리자만(타모임멘토·멘티 403).
- **크로스모듈**: EnrollmentService 선례대로 서비스 주입, 타 모듈 테이블 직접 접근 없음. survey_answer.question_id는 SurveyQuestionDto.id로 키.

## 검증 결과
- **백엔드(비통합)**: `gradlew test` **162 테스트 0 실패**, JaCoCo **line 90.3%**(692/766, ≥80% floor). Spotless/Checkstyle clean.
- **프론트엔드**: build 타입에러 0, **96 테스트/21 파일 0 실패**, coverage **93.96%**, lint 0.
- **아키텍처 리뷰 READY**(게이팅·인가 경계·순환없음·additive id·계약 정합 확인).
- 통합 테스트(SurveyIntegrationTest 등)는 환경상 미실행(Windows/Rancher JNA) — 코드 검토 완료, 존치. 라이브 E2E로 보완 가능.

## Bolt 6/8 이월
- 멘티 현황 세션 일정(U5/Bolt 6), U9 모니터링(Bolt 8). 과정설문 고정 문항 구조(현재 자유 서술).
