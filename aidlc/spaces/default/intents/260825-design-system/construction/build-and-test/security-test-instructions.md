# Security Test Instructions — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(devsecops 지원). 출처: code-generation code-summary.md, intent-statement.md, project.md(Forbidden/Mandated — 외부 SaaS 금지·시크릿 비커밋). 프론트 전용 시각 변경. -->

## 개요 (devsecops 관점)

시각 변경은 write 경로·인증·데이터 처리를 건드리지 않아 신규 공격면이 사실상 없다. 점검은 (a) 외부 의존/자산 유입, (b) 신규 의존성 위생, (c) 시크릿·인가 무변경에 집중.

## 점검 항목

- **외부 SaaS/CDN 유입 없음(C2 준수):** 폰트는 `pretendard` npm 패키지를 self-host — Vite가 woff2를 로컬 번들. 런타임 외부 CDN 호출 없음. Toaster(sonner)·Radix primitive 모두 로컬 번들.
- **신규 의존성 위생:** `pretendard@1.3.9`, `sonner@2.0.8`, `@radix-ui/react-{tooltip@1.2.16,avatar@1.2.6,dropdown-menu@2.1.24}` — 널리 쓰이는 유지보수 패키지, 정확한 버전 고정. 기존 Radix 스택과 동류.
- **인가/인증 무변경:** RequireAuth/RequireRole·API client 인증 헤더·401 처리 로직 미변경(시각 한정). admin 라우트 가드 그대로.
- **시크릿 비노출:** 토큰/폰트/컴포넌트 변경에 비밀값 없음. `.env`류 미커밋(gitignore) 유지.
- **입력 처리 무변경:** controlled-input·검증 로직 미수정(rawtext 규칙 보존).

## 결과

- 신규 취약 도입 없음. `npm audit`의 기존 advisory는 이번 변경과 무관(사전 존재).

## Assumptions & Open Questions

- **[decided]** 시각 한정 변경 — 인증/인가/데이터 write 무변경, 신규 공격면 없음.
- **[assumption]** 추가 primitive는 페이지 미배선 — 렌더 표면 확대 없음.
