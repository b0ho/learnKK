# Team Assessment — learnKK (런크크)

<!-- 팀 가용성·구성 평가. 출처: 사용자 답변(Team Q1~Q4), scope-document, intent-backlog, feasibility-assessment. -->

## 팀 구성

- **인원:** 개발자 3인 (Dev1, Dev2, Dev3).
- **역량:** 3인 모두 비슷한 **풀스택** 수준 — React(프론트) + Java Spring(백엔드) + PostgreSQL 모두 다룰 수 있음 (Team Q1=D).
- **외부 인력:** 없음 (Team Q4=A).
- **위치·시간대:** 동일 팀, 특이사항 없음 (Team Q4=A).

## 작업 방식

- **분배 방식:** 기능 수직 슬라이스 — 각자 일부 proto-Unit을 프론트~백엔드까지 통째로 담당 (Team Q2=A). `intent-backlog.md`의 11개 Must proto-Unit을 3개 세로 묶음으로 배분.
- **협업 방식:** 독립 병렬 — 각자 자기 Bolt를 독립 구현하고, 단위 간 인터페이스만 사전 합의 (Team Q3=A).
- **구현 실행:** 각자 배분된 Bolt를 개별 구현 워크플로우(`/aidlc`)로 진행 (이번 설계 워크플로우의 산출물을 입력으로).

## 용량·리스크

- 3인 모두 풀스택이라 수직 슬라이스 배분에 스킬 갭 없음 (`feasibility-assessment.md`의 저위험 판정과 일관).
- 독립 병렬의 핵심 리스크는 단위 간 인터페이스 불일치 → 공통 기반(인증·모임 도메인·DB 스키마)을 먼저 계약(interface contract)으로 고정하는 것이 전제. 구체 순서는 delivery-planning에서 확정.

## Assumptions & Open Questions

- Dev1/2/3의 구체 식별·이름은 미정(균등 풀스택 가정). 실제 배정은 팀이 확정.
- proto-Unit → 개발자 매핑은 units-generation·delivery-planning에서 최종 확정(여기서는 제안 수준).
