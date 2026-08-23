# Unit Test Instructions — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard. test-alongside로 동반 생성됨. -->

## 프레임워크·설정
- 백엔드: JUnit 5 + Mockito + `@WebMvcTest`(MockMvc, @MockBean AuthService). JaCoCo(80% line). 프론트: Vitest + RTL(80%).

## 실행
- 백엔드: `cd backend && ./gradlew test jacocoTestReport`. 프론트: `cd frontend && npm run test -- --run --coverage`.

## 커버리지 대상 (Bolt 7)
| 컴포넌트 | 초점 |
|---|---|
| PreSurveyServiceTest | submit 정상·②전(NOT_OPEN)409·비참여자403·필수누락400·재제출 upsert; getAnswers 소유멘토/관리자/본인200·타인403·COMPLETED후 열람 |
| FeedbackServiceTest | submit 정상·비참여자·상태위반; listFeedback 소유멘토/관리자200·타모임멘토403·멘티403 |
| Survey/FeedbackControllerTest(@WebMvcTest) | 5 라우트 상태코드·인가 |
| FE PreSurveyAnswerPage/FeedbackPage/FeedbackViewPage | ②전 안내·제출·409/400·권한·목록 |
| FE survey/feedback api | 호출 경로 |

## 목표
- BE/FE 각 line ≥80%. 게이팅·인가 분기 커버.
