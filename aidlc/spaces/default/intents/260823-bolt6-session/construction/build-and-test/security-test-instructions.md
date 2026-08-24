# Security Test Instructions — Bolt 6 Session/Attendance (learnKK)

<!-- build-and-test 산출물(devsecops 지원). 입력: construction/bolt6-session/code-generation/code-generation-plan.md·code-summary.md + U5 nfr-requirements/security-requirements.md. -->

## 인가 경계 (BR-U5-6) — 필수 검증
- 세션 생성/변경: 소유 멘토만(비소유·멘티 → 403 SESSION_FORBIDDEN).
- 출석 checkIn: 해당 모임 참여 멘티 본인만(비참여자 → 403 ATTENDANCE_NOT_PARTICIPANT).
- 수료 판정 조회/실행: 소유 멘토 또는 관리자(그 외 403).
- ④ 확정: role=ADMIN만(비관리자 → 403). `/api/admin/**` 프리픽스 + SessionAuthInterceptor 인증 강제.
- 모든 U5 라우트는 인증 필요(토큰 없으면 401 AUTH_UNAUTHENTICATED).

## 검증 방법
- 라이브 E2E에서 각 경계를 음성 케이스로 검증(비소유 멘토 addSession 403, 비참여자 checkIn 403, 비admin approve 403) — build-test-results.md 실측.
- @WebMvcTest 컨트롤러 테스트가 인가 분기 상태코드 커버.

## 데이터·비밀값 (project.md Forbidden)
- 비밀값(DB 자격증명·세션 시크릿) 비커밋 — `.env` gitignore, 환경변수 주입 확인.
- Entity를 API 경계에 노출 금지 — U5 컨트롤러는 record DTO만 반환(확인).
- 입력 검증: CreateSessionRequest/UpdateSessionRequest @Valid(@NotNull·@Min).
