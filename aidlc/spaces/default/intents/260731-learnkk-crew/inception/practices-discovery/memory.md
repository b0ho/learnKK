# Practices Discovery — Observation Diary

<!-- Auto-created at stage start. Maintained by the orchestrator. Not hand-edited by the user. -->

## Interpretations

- 2026-08-01T05:12:00Z - Greenfield 프로젝트로 확인(aidlc-state.md Project Type: Greenfield). org.md 5개 섹션을 suggested default로 사용; team.md는 전부 비어 있어 re-run 컨텍스트 없음.
- 2026-08-17T00:00:00Z — 인터뷰 재개 시 사용자가 "I'll edit the file" 모드로 직접 편집(Q1~Q6 전부 A) 후 "완료" 신호. self-guided 모드로 처리하고 answer를 audit에 단일 QUESTION_ANSWERED로 기록.
- 2026-08-17T00:00:01Z — greenfield라 실측 증거가 없어, 지원 3인 blind review의 "제안" 포지션을 인터뷰 A 확정과 1:1 매핑해 최종 통합. 리뷰 포지션이 곧 확정 practice의 근거가 됨.

## Deviations

## Tradeoffs

- 2026-08-17T00:00:02Z — developer/quality가 제시한 계약 우선(monorepo + OpenAPI/DB/도메인 타입 3계약 + Entity 비노출 + camelCase↔snake_case)을 practice가 아닌 discovered-rules `[affirmed]` hard rule로 승격. 독립 병렬 interface 불일치가 지배 리스크라, durable rule로 못박는 편이 team.md 자세보다 강제력이 크다고 판단.

## Open questions

- 2026-08-17T00:00:03Z — 프론트 빌드 도구(Vite vs webpack/CRA) 미확정 → Vitest vs Jest 러너 선택의 선행 조건. 구현 워크플로우에서 확정 필요.
- 2026-08-17T00:00:04Z — 공유 계약(API/DB/도메인 타입)의 소유자·고정 순서, DB 마이그레이션 도구(Flyway/Liquibase), BE 빌드(Gradle/Maven·모듈 구성)는 delivery-planning/구현 워크플로우로 이월.
