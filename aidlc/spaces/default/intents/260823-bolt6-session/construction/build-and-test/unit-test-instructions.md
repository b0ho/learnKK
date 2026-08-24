# Unit Test Instructions — Bolt 6 Session/Attendance (learnKK)

<!-- build-and-test 산출물. 입력: construction/bolt6-session/code-generation/code-generation-plan.md·code-summary.md. Standard 전략(컴포넌트당 5~8). -->

## 프레임워크·실행
- 백엔드: JUnit 5 + Mockito + AssertJ. 실행 `./gradlew test --tests "com.learnkk.session.*" --tests "com.learnkk.contract.*"`.
- 프론트: Vitest + @testing-library/react. 실행 `npx vitest run` (co-located `*.test.ts(x)`).
- 커버리지: 백엔드 JaCoCo LINE ≥80% floor(`check`), 프론트 vitest thresholds 80%(lines/functions/branches/statements).

## 백엔드 커버 대상 (code-summary 매핑)
- `SessionServiceTest`(9): addSession 소유멘토/비소유403/비IN_PROGRESS409, updateSession, listSessions, allScheduledSessionsEnded(종료/미종료/빈세션 vacuous-true).
- `AttendanceServiceTest`(9): checkIn 정상, 창밖(이르/늦)→409, 비참여자→403, 비활성→409, 멱등; getMyAttendance a/S·**S=0→rate 0**.
- `CompletionServiceTest`(10): **80% 경계(a*100==80*S)**, S=0 보류, 참여자별 판정; ④ 정상·미충족409·이미확정409·비admin403.
- 컨트롤러 3종(@WebMvcTest, 6/5/6): 라우트 상태코드·인가·검증. `OpenApiContractTest`(15): U5 DTO 계약 정합.

## 프론트 커버 대상
- `api/sessions.ts`(8 메서드), MentorHub 세션관리, MenteeLearning 출석 팝업·창밖 409·출석율, AdminApprovalPage ④ 확정 — co-located 테스트.

## 기대치
- 신규 U5 단위+계약 60 테스트 0 실패, 프론트 97 테스트 0 실패, 커버리지 floor 상회(실측 build-test-results.md).
