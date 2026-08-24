# 코드 구조 — learnKK

## 백엔드 (`/backend/src/main/java/com/learnkk`)
- `kernel/` — 공유: `domain/MeetingStatus`, `domain/CompletionStatus`, `error/ErrorCodes`·`GlobalExceptionHandler`·예외 클래스, `security/Principal`·`SessionAuthInterceptor`, `web/PageResponse`.
- `auth/` — `entity/Session`(토큰), `User`, `AuthService`, `SessionAuthInterceptor`(경로 보호 판정), `web/AuthController`.
- `meeting/` — `entity/Meeting`·`SurveyQuestion`, `repository/MeetingRepository`(transitionStatus), `service/MeetingService`·`MeetingApprovalService`(T1~T6)·`SurveyTemplateService`(문항 lock), `service/SessionCompletionGate`(interface), `web/MeetingController`·`MeetingApprovalController`.
- `enrollment/` — `entity/Enrollment`(status APPLIED/CANCELLED), `service/EnrollmentService`(apply/cancel/read 포트), `web/EnrollmentController`.
- `session/` — `entity/MeetingSession`, `repository/MeetingSessionRepository`·`AttendanceRepository`·`MenteeCompletionRepository`, `service/SessionService`·`AttendanceService`·`CompletionService`·`SessionBackedCompletionGate`, `web/SessionController`·`AttendanceController`·`CompletionController`.
- `content/`, `messaging/`, `survey/` — 각 도메인 entity/dto/repository/service/web.

## 프론트엔드 (`/frontend/src`)
- `api/` — 단일 client 계층: `client.ts`(request/downloadFile), `errors.ts`(코드→한글), 도메인별 `meetings.ts`·`enrollments.ts`·`sessions.ts`·`admin.ts`·`survey.ts`·`feedback.ts`·`content.ts`·`messages.ts`, `types.ts`, `session.ts`(토큰 저장).
- `routes/` — `AppRouter.tsx`(라우팅), `AppShell.tsx`(하단 탭 네비), `paths.ts`(경로 상수), `RequireRole`.
- `features/` — `meetings/`(MeetingListPage·MeetingCreatePage·AdminApprovalPage·MyLearningPage·SurveyBuilder·meetingValidation), `content/`(MeetingContentPage), `survey/`(PreSurveyAnswerPage·FeedbackPage·FeedbackViewPage), `messaging/`, `shared/`(meetingStatus·completionStatus).
- `auth/` — `useAuth`(role 제공), `components/ui/`(shadcn 계열: Button·Card·Input·Dialog·Badge·Textarea 등).

## 코드 패턴/분류
- FE 페이지는 역할 적응형(예: `MyLearningPage`가 role===MENTOR면 `MentorHub`, 아니면 `MenteeLearning`).
- 각 도메인 API는 `request<T>(path, {method, body, query, auth})` 한 함수로 소비.
- 백엔드는 record DTO + `from(entity)` 정적 팩토리로 응답 변환.

## 테스트 배치
- FE: 각 컴포넌트 옆 `*.test.tsx`/`*.test.ts` (Vitest + RTL).
- BE: `src/test/java/com/learnkk/**` — 서비스(Mockito), 웹(@WebMvcTest), 통합(Testcontainers), 계약(`contract/OpenApiContractTest`).
