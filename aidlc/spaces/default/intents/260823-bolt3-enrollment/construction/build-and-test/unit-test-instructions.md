# Unit Test Instructions — Bolt 3 Enrollment (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard(컴포넌트당 5~8). test-alongside로 동반 생성됨. -->

## 프레임워크·설정
- 백엔드: JUnit 5 + Mockito + `@WebMvcTest`(MockMvc). JaCoCo(floor 80% line). 프론트: Vitest + RTL(floor 80%).

## 실행
- 백엔드: `cd backend && ./gradlew test jacocoTestReport`. 특정: `--tests "com.learnkk.enrollment.service.EnrollmentServiceTest"`.
- 프론트: `cd frontend && npm run test -- --run --coverage`.

## 커버리지 대상 (Bolt 3)
| 컴포넌트 | 초점 |
|---|---|
| EnrollmentServiceTest | apply 정상·비멘티403·비RECRUITING(NOT_OPEN)409·정원마감(FULL)409·중복(DUPLICATE)409; cancel 정상·②후(CANCEL_FORBIDDEN)409·비본인404; listApplicants 소유자외403 |
| EnrollmentControllerTest(@WebMvcTest) | 4 라우트 상태코드·인가(@MockBean AuthService) |
| FE MeetingListPage.test | 멘티 신청 버튼·409 매핑 |
| FE MyLearningPage.test | 멘티 현황 목록·취소 게이팅 / MentorHub 신청자 |
| FE enrollments.test | api 호출 경로(apply/cancel/listMine/listApplicants) |

## 목표
- BE/FE 각 line ≥80%. 신청/취소/인가 분기 커버.
