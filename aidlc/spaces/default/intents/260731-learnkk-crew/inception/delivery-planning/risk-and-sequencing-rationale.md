# Risk & Sequencing Rationale — learnKK (런크크)

<!-- delivery-planning 산출물. Bolt 순서의 '왜'. 출처: bolt-plan, unit-of-work-dependency(DAG), team-practices, stories, requirements. 휴리스틱: walking-skeleton-first(Cockburn) + 의존성 준수 + 여정 가치. 정량 WSJF 미사용(파일럿). -->

## 사용 휴리스틱

- **Walking skeleton first (Cockburn)** — 첫 Bolt로 공통 기반을 관통해 아키텍처·계약을 검증(team-practices 확정). 독립 병렬의 최대 리스크(interface 불일치)를 착수 전에 제거.
- **의존성 준수** — units-generation DAG(U1→U2→U3→{U4,U6,U7}→{U5,U8}→U9)를 위반하지 않음.
- **여정 가치** — 핵심 수료 여정(개설→모집→진행→출석→수료)을 우선.
- 정량 WSJF/CD3 점수표는 파일럿·소규모라 미사용(서술적 근거로 충분, Q2=A).

## Bolt 순서 근거

1. **Bolt 1 (Walking Skeleton, U1+U2+U3최소)** — 최우선 리스크(Q6=A) = 공유 계약(#1/#2/#3) 선고정. 3인 독립 병렬에서 계약이 흔들리면 병합 시 interface 불일치가 폭발하므로, 계약+인증+최소 모임 흐름을 먼저 관통해 고정. walking-skeleton-first의 핵심.
2. **Bolt 2 (Meeting 완성, U3)** — 상태머신·승인 4지점이 다수 Bolt의 선행 계약(모임 상태). 도메인 복잡도(XL)가 집중되어 조기 안정화 필요. DAG상 U4~U9의 부모.
3. **Bolt 3 (Enrollment, U4)** — 모집·멤버십이 U5(출석 대상)·U8(참여자)의 선행. 정원 동시성 리스크를 중간에 해소.
4. **Bolt 4/5/7 (Content/Messaging/Survey) 병렬** — U3(+U4) 이후 상호 독립. 가치는 있으나 리스크·복잡도 낮아 병렬로 처리량 확보.
5. **Bolt 6 (Session/Attendance, U5)** — 출석·80% 수료가 성공지표의 핵심이나, U4 이후에야 대상(참여자)이 확정되고 시간창 판정 복잡도(L)가 있어 계약 안정 후 진행. 수료 여정 완결의 핵심.
6. **Bolt 8 (Admin/Monitoring, U9)** — 다수 도메인 read 조합이라 상류 Bolt(U3/U4/U5/U8) 완료 후 자연 후행.

## 위상 순서 대비 편차

- Bolt 순서는 units-generation DAG의 **위상 순서를 준수**(편차 없음). walking-skeleton 번들(U1+U2+U3최소)은 위상상 최상위(U1→U2→U3)를 한 Bolt로 묶은 것으로 순서 위배 아님.
- U5(Bolt 6)를 Content/Messaging/Survey(Bolt 4/5/7)보다 뒤에 둔 것은 위상 허용 범위 내 **경제적 선택**(복잡도 분산·수료 여정 후반 집중) — 위상 위배 아님.

## 핵심 리스크 항목 (조기 대응)

| 리스크 | 대응 Bolt | 근거 |
|--------|-----------|------|
| 공유 계약 불일치(#1/#2/#3) | Bolt 1 | 독립 병렬 최대 리스크 — skeleton에서 선고정 |
| 인증·세션·RBAC 기반 | Bolt 1(U2) | 모든 액션의 전제 |
| 상태머신·승인 4지점(③ 관리자 직접) | Bolt 2 | 도메인 복잡도 집중, 불법 전이 409 |
| 정원 동시성(선착순) | Bolt 3 | 잔여 1석 경합·중복 신청 |
| 세션 시간창·80% 수료 경계 | Bolt 6 | 성공지표 핵심, 경계값(a*100≥80*S) |

## Assumptions & Open Questions

- 파일럿·설계 전용이라 순서는 팀 재량 재조정 가능(로드맵 권장안).
- 시간창·사번 형식·bytea/LO 등 미확정은 functional-design.
