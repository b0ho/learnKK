# Code Generation — Stage Diary (ux-bugfixes)

## Interpretations
- 2026-08-24T14:30:00Z — units-generation/functional-design/nfr-design 등 설계 산출물은 bugfix scope에서 생략(consumes_absent, expected). 요구사항(requirements.md) + CodeKB(aidlc/spaces/default/codekb/learnKK) 기반으로 스코프. 단위 이름은 없으므로 단일 단위 `ux-bugfixes` 레코드 디렉터리 사용.
- 2026-08-24T14:30:00Z — 브라운필드: 기존 파일 in-place 수정, 중복 파일 금지. 상호작용 UI에 data-testid 유지/추가.

## Deviations

## Tradeoffs
- 2026-08-24T14:30:00Z — Minimal test strategy(bugfix): 각 수정 영역에 회귀 테스트(요구사항당 happy-path 위주) + 기존 스위트 green 유지. 신규 백엔드(역전이·세션 삭제/완료·재신청·관리자 목록)는 서비스 단위 테스트, 계약 변경은 OpenApiContractTest 갱신.

## Open questions
