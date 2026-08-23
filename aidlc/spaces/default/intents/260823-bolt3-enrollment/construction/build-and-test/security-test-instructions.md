# Security Test Instructions — Bolt 3 Enrollment (learnKK)

<!-- build-and-test 산출물(devsecops 지원). 출처: U4 nfr-requirements/security-requirements.md, project.md. Standard: 인가·무결성 단위/슬라이스 검증. -->

## 초점
Bolt 3 보안 표면 = 인가 경계 + 신청 무결성(정원/중복).

## 검증 항목 (테스트로 커버)
- **인가(RBAC)**: apply=MENTEE 본인만(비멘티 403), cancel=본인만, listApplicants=소유 멘토/ADMIN만(그 외 403), listMyEnrollments=호출자 범위. 미인증 401(SessionAuthInterceptor 신규 4 라우트 보호).
- **무결성**: 어드바이저리 락 + count + `UNIQUE(meeting_id,mentee_id)`로 overbooking·중복 차단(권한 있어도 초과 신청 불가).
- **입력/상태 검증**: 비RECRUITING 신청 409, ②후 취소 409.
- **정보 노출 최소화**: ApplicantResponse는 최소 멘티 정보(닉네임). 전역 ErrorPayload(스택트레이스 비노출).
- **시크릿**: `.env` 주입·비커밋(project.md).

## 실행
- 인증/인가 경계는 서비스 단위(Mockito) + `@WebMvcTest` + 통합/라이브 E2E의 403/401/409 케이스로 검증.
- 정식 SAST/DAST는 CI 스코프(이번 SKIP) — 이월.
