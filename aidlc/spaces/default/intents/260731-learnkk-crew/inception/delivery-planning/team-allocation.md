# Team Allocation — learnKK (런크크)

<!-- delivery-planning 산출물. Bolt↔개발자(mob) 배정. 출처: team-formation(mob-composition Dev1/2/3, 독립 병렬), unit-of-work, bolt-plan. team-formation(1.5)이 실행됨 → 실제 팀(3인) 참조. -->

## 팀 구성 (team-formation 1.5)

- 개발자 3인, 전원 풀스택(React+Spring+PostgreSQL). 협업 토폴로지: **독립 병렬**(mob/pair 아님) — 각자 수직 슬라이스, 인터페이스만 합의(team-practices).
- 이번 스코프는 설계 전용 — 아래 배정은 팀 3인의 **후속 구현 워크플로우** 소유 기준. (이 워크플로우의 설계 문서는 AI가 unit-major로 작성.)

## Bolt ↔ 개발자 배정

team-formation mob-composition의 Dev1/2/3 배분을 9 Unit·Bolt에 매핑:

| 개발자 | 소유 Unit | 소유 Bolt | 성격 |
|--------|-----------|-----------|------|
| **Dev1 (기반·회원·모임)** | U1 Contracts&Kernel, U2 Auth&Shell, U3 Meeting | Bolt 1(skeleton 주도) + Bolt 2 | 공통 기반·계약·상태머신 — 선행·계약 소유 |
| **Dev2 (모집·진행·소통)** | U4 Enrollment, U6 Content, U7 Messaging | Bolt 3 + Bolt 4 + Bolt 5 | 모집·자료·쪽지 |
| **Dev3 (출석·수료·모니터링)** | U5 Session/Attendance, U8 Survey/Feedback, U9 Admin/Monitoring | Bolt 6 + Bolt 7 + Bolt 8 | 출석·수료·설문·모니터링 |

## 협업·게이트

- **Bolt 1(walking skeleton)은 3인 공동 참여 권장** — 공통 계약(#1/#2/#3) 합의가 핵심이므로 skeleton은 함께 세우고, 이후 각자 슬라이스로 분기(team-practices).
- Bolt 1 이후: 의존성 없는 Bolt는 3인 병렬. Dev1의 U3(Bolt 2)가 다수 Bolt의 선행이므로 Dev1 계약·상태머신 우선.
- 통합·인터페이스 조율은 3인 공동(mob-composition RACI: 담당=R/A, 나머지=Consulted).

## Program Board (팀 수 > 1)

- 3인 병렬이므로 Program Board 관점: Bolt 1(공동) → 이후 3 레인(Dev1/2/3) 병렬. 레인 간 동기화 지점 = 공유 계약 변경 시.

## Note (설계 전용 워크플로우)

- 이번 AI-DLC 워크플로우는 code-gen을 실행하지 않음 → 위 배정은 후속 구현 워크플로우가 상속. 이 워크플로우의 functional-design·nfr-requirements는 aidlc-developer 등 AI가 unit-major로 수행.

## Assumptions & Open Questions

- 실제 개발자 식별·역량 매칭은 팀이 구현 착수 시 확정(현재는 역할 기반 배정).
- Bolt 병렬 배치·재조정은 팀 재량(bolt-plan은 권장 시퀀스).
