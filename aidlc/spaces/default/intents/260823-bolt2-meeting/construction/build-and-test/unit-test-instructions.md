# Unit Test Instructions — Bolt 2 Meeting 완성 (learnKK)

<!-- build-and-test 산출물(quality 리드). Test Strategy=Standard(컴포넌트당 5~8). 출처: code-generation-plan.md·code-summary.md, team.md Testing Posture(80% floor). code-generation에서 test-alongside로 동반 생성됨. -->

## 프레임워크·설정

- **백엔드**: JUnit 5 + Mockito(순수 단위) + Spring `@WebMvcTest`(컨트롤러 슬라이스, MockMvc). JaCoCo 커버리지(`jacocoTestReport`, floor 80% line).
- **프론트엔드**: Vitest + React Testing Library + `@testing-library/user-event`. v8 coverage(floor 80%).

## 실행 방법

- 백엔드 전체 단위/슬라이스: `cd backend && ./gradlew test jacocoTestReport`. 특정 클래스: `--tests "com.learnkk.meeting.service.MeetingApprovalServiceTest"`.
- 프론트엔드: `cd frontend && npm run test -- --run` (coverage: `npm run test -- --run --coverage`).

## 커버리지 대상 (Bolt 2 신규·확장)

| 컴포넌트 | 테스트 초점 |
|----------|-------------|
| `MeetingApprovalServiceTest` | T3 진행/T4 취소(사유 필수 400)·비RECRUITING 409, T5 정상·비READY 409, T6 정상·gate-false 409(MEETING_SESSIONS_NOT_ENDED)·비IN_PROGRESS 409, 비관리자 403, 이중 전이 경합 409 |
| `MeetingServiceTest` | listMyMeetings 자기 모임 필터·비멘토 403 |
| `SurveyTemplateServiceTest` | ②시작(IN_PROGRESS)/COMPLETED/CANCELLED 문항 잠금 409, READY_TO_START 편집 가능(BR-U3-7) |
| `NoSessionsCompletionGateTest` | Bolt 2 스텁 통과(true) 반환 |
| `MeetingControllerTest`/`MeetingApprovalControllerTest` | 신규 라우트 상태코드·인가(@WebMvcTest) |
| FE `AdminApprovalPage.test.tsx` | status별 액션 버튼·모집확정 다이얼로그·409 매핑 |
| FE `MyLearningPage.test.tsx` | MentorHub listMine 렌더·상태 뱃지·빈/에러 |
| FE `admin.test.ts` | 신규 admin/meetings API 호출 경로 |

## 커버리지 목표

- BE/FE 각 **line ≥80%**(팀 floor). 전이 분기(정상/불법409/경합/인가)를 상태별로 커버.
