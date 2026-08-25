# Build & Test Results — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). 출처: code-generation code-summary.md·code-generation-plan.md, intent-statement.md. 프론트 전용 시각 변경 — 실제 실행 결과. -->

## 개요

디자인 시스템(그린 브랜드 토큰 + Pretendard variable + components/ui 폴리시) 적용 후 프론트엔드 빌드·테스트·린트·라이브 렌더를 실행한 결과. 백엔드 변경 없음.

## 실행 결과

| 검증 | 명령 | 결과 |
|------|------|------|
| 단위/컴포넌트 테스트 | `npx vitest run` | ✅ **135 passed / 135** (28 files) — 베이스라인과 동일, 회귀 0 |
| 프로덕션 빌드 | `npm run build` (`tsc -b && vite build`) | ✅ **green**. 사전 tsc 에러 3건 정리 후 통과 |
| 린트 | `npm run lint` (eslint) | ✅ **0 errors** (1 warning: SurveyBuilder.tsx react-refresh — 사전 존재, 무관) |
| 라이브 렌더 스모크 | vite dev + 브라우저(모바일 390px) | ✅ 로그인/가입 화면 그린 브랜드·Pretendard·카드/버튼/라디오 폴리시 렌더 확인 |

## 번들(빌드 산출)

- `PretendardVariable-*.woff2` 단일 ~2.06MB (static 9종 ~13MB에서 축소, 모바일 우선 NFR1).
- `index-*.js` 370.32KB (gzip 115.64KB), `index-*.css` 23.93KB (gzip 5.31KB).

## 회귀 판정

- 기존 135 테스트 전부 통과 — data-testid·role="alert"·한글 상태 문구 계약 보존 확인.
- 기능·라우팅·API 무변경(시각 한정) — 동작 회귀 없음.

## Assumptions & Open Questions

- **[decided]** 프론트 전용 변경 — 백엔드 빌드/테스트는 이번 범위 대상 아님(무변경).
- **[env]** 백엔드 통합 테스트(Testcontainers)는 이 변경과 무관하므로 미실행.
