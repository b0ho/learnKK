# Tech Stack Decisions — U2 Auth & App Shell (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U2(service, FE 셸 포함). 출처: business-logic-model.md(세션·FE 셸·단일 API client), business-rules.md(BR-U2-3 서버세션·bcrypt), requirements.md(C1 스택·NFR1 모바일 웹뷰·NFR6). U1 tech-stack-decisions(스택·계약 도구) 상속. U2는 인증·FE 셸의 구체 기술 선택을 확정. -->

## 개요

U1에서 상속한 스택(React+Spring+PostgreSQL)·계약 도구 위에, U2는 인증·세션·FE 앱 셸의 **구체 기술 선택**을 확정한다. U1이 위임한 [open] 항목(세션 저장·Spring Security 구성)을 여기서 확정.

## 백엔드 기술 선택

### TD-U2-1. 인증 프레임워크 — Spring Security

- **결정:** Spring Security로 인증 필터·인가·비밀번호 인코딩(bcrypt `BCryptPasswordEncoder`) 구성.
- **근거:** Spring 표준, bcrypt 내장, 필터 체인으로 세션 검증 전처리 자연스러움.

### TD-U2-2. 세션 저장 — 서버 세션(DB) (U1 [open] 확정)

- **결정:** DB `session` 테이블 기반 서버 세션(토큰→세션 조회). JWT 미채택.
- **근거:** 단일 인스턴스(services.md)라 조회 부담 없음, 로그아웃 즉시 무효화가 단순(JWT blocklist 불요). business-rules BR-U2-3.
- **Reversibility:** 중간 — 수평 확장 시 재검토(scalability).

### TD-U2-3. 비밀번호 해시 — bcrypt

- **결정:** bcrypt(FR1.2 [Mandated]), cost 인자 10~12 [assumption]. Spring Security `BCryptPasswordEncoder`.

## 프론트엔드 기술 선택

### TD-U2-4. UI 라이브러리 — React + shadcn/ui

- **결정:** React + TypeScript, shadcn/ui 컴포넌트(refined-mockups design-system). 모바일 웹뷰 최적화(NFR1).
- **근거:** components.md FE 구조, 공통 UI 셋업을 앱 셸이 소유.

### TD-U2-5. 단일 API client — fetch 래퍼

- **결정:** 단일 API client 계층(fetch 래퍼) — 인증 헤더 자동 첨부, ErrorPayload(U1) 해석, 401 세션 만료 처리. 전 feature 공유.
- **근거:** components.md `api/`, 중복 제거·에러 처리 일원화.

### TD-U2-6. 라우팅·상태

- **결정:** 클라이언트 라우팅(3탭 + 인증 라우트), 서버 상태는 fetch 캐시 계층, 전역 UI 상태 최소(components.md).
- **테스트:** React Testing Library + Vitest(team-practices, Vite 가정 [open]).

## 범위 밖

- CI/CD·배포·운영 모니터링(C3), 운영 TLS·httpOnly 쿠키 전환(후속 하드닝).

## Assumptions & Open Questions

- **[assumption]** bcrypt cost 10~12, sessionStorage 토큰, 세션 TTL.
- **[open]** FE 빌드 도구(Vite vs webpack/CRA) → 테스트 러너(Vitest vs Jest), 수평 확장 시 세션 스토어.
- Spring Security 세부 구성(필터 순서 등)은 구현.
