# Build and Test — Stage Diary (ux-bugfixes)

## Interpretations
- 2026-08-24T15:20:00Z — Minimal test strategy(bugfix): unit-test-instructions 중심. 나머지 instruction 파일은 required-sections 충족용으로 작성하되 "해당 없음/범위 밖" 근거를 명시.

## Deviations
- 2026-08-24T15:20:00Z — 통합 테스트(Testcontainers)는 이 환경에서 Docker 클라이언트 초기화 실패로 실행 불가 → 실행 불가 사유를 build-test-results에 기록하고 진행. 회귀 검증은 단위/웹/계약(285 green) + 프론트(28 파일 green) + 라이브 부팅/스모크로 대체.

## Tradeoffs
- 2026-08-24T15:20:00Z — 통합 테스트를 강제로 통과시키려 시도하지 않음(환경 이슈이며 버그픽스와 무관, 안 건드린 Messaging/Meeting 통합도 동일 실패). 로컬 라이브 검증으로 신규 엔드포인트 동작을 확인함.

## Open questions
- 2026-08-24T15:20:00Z — Testcontainers가 JDK21 + 현재 Docker에서 왜 Unsafe 초기화 오류를 내는지는 별도 환경 조사 대상(범위 밖).
