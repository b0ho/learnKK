# Security Test Instructions — Bolt 1 (learnKK)

<!-- build-and-test 산출물(devsecops 지원 관점). 출처: code-generation-plan.md·code-summary.md(auth/세션/RBAC), U2 security-requirements(bcrypt·세션·열거 방지), project.md(bcrypt Mandated·시크릿 비커밋 Forbidden). Standard 전략에선 필수 아니나 auth 슬라이스라 인증/인가 검증을 명시. -->

## 인증·비밀번호

- 비밀번호는 bcrypt(Spring `PasswordEncoder`)로만 저장 — 평문·가역 저장 없음(project.md Mandated). 저장 값이 해시인지 확인.
- 로그인 실패는 계정 존재 비특정(동일 401 메시지) — 사용자 열거 방지(U2 BR-U2-3). 단위 테스트로 검증됨.
- 세션 토큰: 만료(`expiresAt`)·revoked 검증, 무효 토큰 401. 로그아웃 즉시 무효화.

## 인가 (RBAC 경계)

- 무인증 보호 라우트 접근 401. 역할 게이트: MENTOR만 개설(403 else), ADMIN만 승인/반려(403 else). 본인 프로필만 수정(403 else). 단위/통합 테스트로 검증.
- 향후(Bolt 2+): 소유 경계(자기 모임 멘티/신청자 열람) 확장 시 403 경계 테스트 추가.

## 시크릿·구성

- `.env` gitignore, 커밋된 시크릿 없음(`.env.example`만). `git ls-files`로 추적 `.env` 부재 확인.
- DB 자격증명·세션 시크릿은 환경변수/Spring profile 주입.

## 후속(범위 밖, Bolt 2+/후속 워크플로우)

- SAST/DAST·의존성 스캔(team.md 언급)·인젝션 테스트는 CI 파이프라인(이번 스코프 SKIP, project.md Scope Overrides)로 이월. 파일럿 로컬 범위에서는 인증/인가 단위·통합 검증으로 충당.
