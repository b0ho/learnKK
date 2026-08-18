# Performance Test Instructions — Bolt 1 (learnKK)

<!-- build-and-test 산출물. 출처: code-generation-plan.md·code-summary.md, U2/U3 performance-requirements(NFR3 체감 1~2초·파일럿 규모 NFR2), services.md(단일 인스턴스). Standard 전략·파일럿 규모라 엄격 부하 테스트는 범위 밖 — 가이드 수준. -->

## 범위·목표

- 파일럿 규모(NFR2: 동시 수십 명·모임 수십 개), 단일 인스턴스. 엄격 SLA 아님 — 화면/목록 응답 체감 1~2초(NFR3 가이드).
- 정식 부하/벤치마크·회귀 감지는 후속 performance-validation(operation phase)로 이월 — 이번 스코프에서 operation은 SKIP(project.md Scope Overrides).

## 이번 Bolt 확인 항목 (경량)

- listRecruiting 페이지네이션(size 상한 clamp)으로 대량 응답 방지 — 목록 응답 크기 경계 확인.
- 세션 검증은 인터셉터 단일 DB 조회(단일 인스턴스라 부담 낮음).
- 조건부 UPDATE(상태 전이)로 락 경합 최소화 — 이중 승인 시 하나만 성공(정확성이 우선, 성능 부수).

## 실행(선택)

- 로컬 수동: `curl`/브라우저로 로그인·목록·개설·승인 왕복 시간 관찰. 자동 부하 도구(k6/Gatling)는 후속 워크플로우.
