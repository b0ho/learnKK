# Build & Test Summary — apply-button-state

## 결론
이번 버그픽스(FR-1 신청 상태 반영, FR-2 정원 마감 표기, FR-3 우선순위, FR-4 계약/타입)는 **검증 완료(green)**. 잔여 red 2건은 모두 사전 존재·환경성으로 이번 변경과 무관.

## 검증 매트릭스
| 항목 | 결과 |
|---|---|
| Backend compile (main+test) | PASS |
| Backend unit/web/contract (293) | PASS (신규 회귀 포함) |
| Backend integration (21) | FAIL — Testcontainers Docker 초기화(환경, 무관) |
| Frontend tsc --noEmit | PASS |
| Frontend vitest (137) | PASS (신규 2 포함) |
| Frontend eslint | 0 errors (1 사전 경고) |
| Frontend prod build (tsc -b) | PASS (AppShell.tsx 사전 타입에러 Q1=A로 함께 수리) |

## 게이트 결정 반영
- 통합테스트 Docker 초기화 실패(21) — Q2=A: 환경 제약으로 수용, 정상 Docker 환경 재실행 권장(코드 무관).
- `AppShell.tsx` 프로덕션 빌드 타입에러 — Q1=A: 함께 수리(`readonly string[]`), 빌드 green 복구.

## 산출물
build-instructions / unit-/integration-/performance-/security-test-instructions / build-test-results / (본 요약)
