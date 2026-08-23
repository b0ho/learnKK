# Security Test Instructions — Bolt 2 Meeting 완성 (learnKK)

<!-- build-and-test 산출물(devsecops 지원). 출처: code-generation-plan.md·code-summary.md, U3 nfr-requirements/security-requirements.md, project.md(전역 에러·시크릿 비커밋). Standard: 인증/인가 단위·슬라이스로 검증(별도 DAST 이월). -->

## 초점

Bolt 2는 관리자 전이 액션(모집확정/②시작/③완료)과 멘토 운영 허브를 추가한다. 보안 표면은 **인가 경계**와 **상태 전이 무결성**.

## 검증 항목 (테스트로 커버)

- **인가(RBAC)**: 모든 admin 전이(`confirm-recruitment`/`approve-start`/`complete`)는 `requireAdmin` → 비관리자 403. `listMyMeetings`는 멘토 전용 403. 미인증 요청 401(SessionAuthInterceptor).
- **상태 전이 무결성**: 조건부 UPDATE(`WHERE status=:from`)로 경합·이중 전이·불법 소스 상태를 409로 차단 → 권한 있는 사용자라도 불법 전이 불가.
- **입력 검증**: 반려/취소 사유 `@NotBlank`(400). `ConfirmRecruitmentRequest.proceed` `@NotNull`.
- **정보 노출 최소화**: 전역 `@RestControllerAdvice`가 ErrorPayload{code,message(한국어),details}로 통일 — 스택트레이스·내부 정보 비노출.
- **시크릿**: `.env` 주입, 커밋 금지(project.md). 세션 토큰은 `Authorization: Bearer` 불투명 토큰.

## 실행 방법

- 인증/인가 경계는 서비스 단위(Mockito) + 컨트롤러 `@WebMvcTest` + 통합(Testcontainers)로 커버 — unit/integration 지시서의 403/401/409 케이스가 보안 검증을 겸함.
- 정식 DAST/SAST 파이프라인은 CI 스코프(이번 SKIP) — 이월.

## 이월

- 자동화 SAST(예: SpotBugs/Semgrep)·의존성 스캐닝은 CI 파이프라인(후속 워크플로우).
