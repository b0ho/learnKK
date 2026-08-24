# Security Test Instructions — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(devsecops 지원). Standard: 인가·게이팅 단위/슬라이스 검증. -->

## 초점
Bolt 7 보안 표면 = 데이터 열람 권한 경계(사전설문 응답·피드백)와 제출 자격.

## 검증 항목 (테스트로 커버)
- **인가(RBAC/소유권)**: 사전설문 응답 열람=소유멘토/관리자/본인(타인 403); 피드백 열람=소유멘토/관리자만(타모임멘토·멘티 403); 제출=참여멘티 본인(비참여자 403). 미인증 401(SessionAuthInterceptor 신규 5 라우트 보호).
- **게이팅**: 사전설문 제출은 IN_PROGRESS에만(409 PRESURVEY_NOT_OPEN) — 권한 있어도 시점 위반 차단.
- **입력 검증**: 필수 문항 누락 400.
- **정보 노출 최소화**: 응답/피드백은 소유 경계 내에서만. 전역 ErrorPayload(스택트레이스 비노출).
- **시크릿**: `.env` 비커밋.

## 실행
- 서비스 단위(Mockito) + `@WebMvcTest` + 통합/라이브 E2E의 403/401/409/400 케이스로 검증.
- 정식 SAST/DAST는 CI 스코프(SKIP) 이월.
