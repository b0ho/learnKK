# Work Composition — learnKK (런크크)

<!-- 작업 구성·배분 제안(독립 병렬, 기능 수직 슬라이스). 출처: 사용자 답변(Team Q2~Q3), intent-backlog, scope-document. mob이 아닌 독립 병렬이므로 배분 제안으로 작성. -->

## 협업 토폴로지

- **독립 병렬(parallel)** — mob/pair가 아니라, 3인이 각자 세로 슬라이스를 독립 구현하고 인터페이스만 합의 (Team Q3=A).

## 제안 배분 (11개 Must proto-Unit → 3인)

`intent-backlog.md`의 의존성 우선 순서를 고려한 **제안**(delivery-planning에서 확정):

| 개발자 | 담당 proto-Unit | 성격 |
|--------|----------------|------|
| Dev1 (기반·회원·모임) | 1 회원·인증·프로필, 2 모임 개설·관리, 5 모임 목록·탐색 | 다른 단위의 기반(도메인·인증) — 먼저 인터페이스 고정 |
| Dev2 (모집·진행·소통) | 3 모임 승인, 4 모집·신청·설문, 6 자료실·공지, 7 쪽지 | 모임 라이프사이클 중반 |
| Dev3 (출석·수료·모니터링) | 8 출석, 9 수료 판정, 10 설문·피드백, 11 관리자 모니터링 | 라이프사이클 후반 + cross-cutting 모니터링 |

## 인터페이스 합의 지점 (독립 병렬 전제)

- **공통 기반 우선 고정:** Dev1의 회원·인증(Unit 1)과 모임 도메인·DB 스키마(Unit 2)는 모든 단위가 의존 → 프로젝트 착수 시 API·스키마 계약을 먼저 합의.
- Unit 4(모집·신청) → Unit 6/7/8이 참조하는 "모임-멤버십" 상태 계약 합의.
- Unit 8(출석) → Unit 9(수료)·Unit 11(모니터링)이 참조하는 "출석 기록" 계약 합의.

## RACI (요약)

- 각 proto-Unit: 담당 개발자 = Responsible/Accountable, 나머지 2인 = Consulted(인터페이스), 시스템 관리자 요구는 Informed.
- 통합·인터페이스 조율 = 3인 공동.

## Assumptions & Open Questions

- 위 배분은 제안이며, units-generation의 정식 Unit 확정과 delivery-planning의 Bolt 순서에서 최종화된다.
- 의존성상 Dev1의 기반 단위가 선행되어야 하므로, 완전 동시 시작보다 "기반 계약 → 병렬 전개" 흐름을 권장.
