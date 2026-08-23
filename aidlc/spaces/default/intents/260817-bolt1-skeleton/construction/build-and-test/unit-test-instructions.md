# Unit Test Instructions — Bolt 1 (learnKK)

<!-- build-and-test 산출물. Test Strategy=Standard(컴포넌트당 5~8 테스트). 출처: code-generation-plan.md(단계별 테스트)·code-summary.md(생성 테스트 목록), team.md Testing Posture(JUnit5/MockMvc/Mockito·Vitest/RTL·80% floor + 도메인 분기). -->

## 프레임워크·실행

- **백엔드**: JUnit 5 + Spring Boot Test + Mockito(서비스 격리) + MockMvc(`@WebMvcTest`, 웹 계층). 실행 `./gradlew test`. 커버리지 `./gradlew jacocoTestReport`(리포트 `build/reports/jacoco/test/`).
- **프론트엔드**: Vitest + React Testing Library + `@testing-library/user-event`. 실행 `npm run test -- --run`. 커버리지 `npm run test:coverage -- --run`.

## 백엔드 단위 테스트 범위 (컴포넌트별)

- **AuthService**: 정상 가입, 중복 사번 409(DUPLICATE_EMPLOYEE_NO), 중복 닉네임 409(DUPLICATE_NICKNAME), ADMIN 가입 400(ADMIN_SIGNUP_FORBIDDEN), 로그인 성공, 로그인 실패 401(열거 방지), 세션 만료/revoked 401.
- **UserService**: 프로필 조회·수정, 본인 아님 403, 태그>10·소개>500 400.
- **MeetingService**: 개설 정상(PENDING_APPROVAL), 비멘토 403, 검증 실패 400, listRecruiting 필터.
- **MeetingApprovalService**: ①승인(PENDING_APPROVAL→RECRUITING), 잘못된 상태 409, 반려, 이중 승인 409.
- **SurveyTemplateService**: 문항 upsert, IN_PROGRESS 이후 편집 금지 409, 소유 멘토 아님 403.
- **PageRequestFactory**: size clamp, 허용 안 된 sort 400.
- **@WebMvcTest**(Auth/User, Meeting/Approval): 요청/응답 스키마·상태코드 매핑.

## 프론트엔드 단위/컴포넌트 테스트 범위

- **api/client·errors**: 인증 헤더 첨부, ErrorPayload→ApiError 해석, 401 처리, 서버 message 우선.
- **auth/validation·labels·meetingValidation**: 클라이언트 검증(사번·비번·weeks/capacity·기간).
- **화면**(Login/Signup/Profile/MeetingList/MeetingCreate/SurveyBuilder/AdminApproval/MyLearning/AppRouter): 상호작용·검증·서버 에러(401/409) 매핑·로딩/빈/에러 상태.

## 커버리지 목표

- 백엔드/프론트 각각 line coverage ≥80% floor(team.md). 도메인 규칙 분기(①승인·불법 전이 409·중복 409·403·401)는 시나리오로 별도 검증(always-pass 금지, construction.md).
