# Delivery Planning — 계획 질문지 (learnKK / 런크크)

이 스코프는 **설계 전용**(construction에서 functional-design·nfr-requirements만 실행, code-gen 이후는 SKIP). 따라서 Bolt 계획은 팀 3인의 **후속 구현 워크플로우가 상속할 시퀀스 로드맵**입니다. units-generation DAG + team-practices(walking skeleton·수직 슬라이스) + team-formation 배분(Dev1/2/3)을 전제로 확정합니다.

각 `[Answer]:`에 보기 문자. 복수 선택 표시. 직접 서술은 `X. 기타`.

---

## Q1. Bolt 시퀀싱 휴리스틱
- A. (권장) **walking-skeleton-first + 이후 의존성/가치 하이브리드** — 첫 Bolt는 공통 기반 관통 슬라이스(U1 계약 + U2 인증·셸 + U3 최소 모임 흐름), 이후 의존성 순서 안에서 가치 높은 여정부터. (team-practices walking skeleton 확정)
- B. 순수 의존성(위상) 순서만
- C. 순수 가치-우선(WSJF)
- X. 기타

[Answer]:a

## Q2. WSJF 등 정량 점수 모델
- A. (권장) **비정량(파일럿)** — walking-skeleton-first + 의존성 + 여정 가치로 서술적 근거. 정식 WSJF 점수표는 과함.
- B. WSJF 점수표 사용(risk/value/size 가중치 지정)
- X. 기타

[Answer]:a

## Q3. Bolt 세분화(granularity)
- A. (권장) **walking skeleton은 U1+U2(+U3 최소)를 한 Bolt로 번들**, 이후 도메인 Unit당 1 Bolt(U3 잔여~U9). 3인 소유와 정렬.
- B. 모든 Unit을 각각 1 Bolt(번들 없음)
- C. Dev별로 크게 묶어 3 Bolt
- X. 기타

[Answer]:a

## Q4. Bolt 병렬 실행
- A. (권장) walking skeleton(Bolt 1)은 **단독 선행**, 이후 의존성 없는 Bolt들은 **3인 병렬** 실행 가능. (team-practices 독립 병렬)
- B. 엄격 순차(한 번에 하나)
- X. 기타

[Answer]:a

## Q5. 외부 의존성
- A. (권장) **없음** — 전부 로컬·팀 자체 완결(외부 API·데이터·승인·외부팀 핸드오프 없음). external-dependency-map은 경량/빈.
- B. 있음(직접 서술)
- X. 기타

[Answer]:a

## Q6. 가장 먼저 다뤄야 할 핵심 리스크 (해당되는 것 모두 선택 가능)
- A. (권장) **공유 계약 선고정**(#1 OpenAPI·#2 DB 스키마·#3 도메인 타입/상태머신) — 독립 병렬 interface 불일치 최대 리스크
- B. 인증·세션·RBAC 기반(U2) — 모든 액션의 전제
- C. 상태머신·승인 4지점(U3) — 도메인 복잡도 집중
- D. 세션 시간 판정·출석·80% 수료 경계(U5)
- X. 기타

[Answer]:a

## Q7. Construction 설계 반복 방식 (functional-design·nfr-requirements 순서)
- A. (권장) **unit-major** — Unit별로 그 Unit의 설계 문서(functional-design→nfr-requirements)를 연속 작성 후 다음 Unit. walking-skeleton/수직 슬라이스에 정합(Unit별 설계 응집).
- B. stage-major(기본) — functional-design을 전 Unit에 대해 먼저, 그다음 nfr-requirements 전 Unit.
- X. 기타

[Answer]:a

---

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation

정리된 답변(전부 A):
- Q1 = A — walking-skeleton-first + 의존성/가치 하이브리드.
- Q2 = A — 비정량(파일럿), 서술적 근거.
- Q3 = A — Bolt 1=walking skeleton(U1+U2+U3 최소), 이후 도메인 Unit당 1 Bolt.
- Q4 = A — skeleton 선행, 이후 독립 Bolt 3인 병렬.
- Q5 = A — 외부 의존성 없음(경량 map).
- Q6 = A — 최우선 리스크=공유 계약 선고정(#1/#2/#3·상태머신).
- Q7 = A — construction 반복 **unit-major**(Unit별 설계 문서 연속).

프롬프트: "이대로 delivery-planning 산출물(bolt-plan/team-allocation/risk-and-sequencing-rationale/external-dependency-map)을 생성하고 construction 반복을 unit-major로 설정할까요?"
- A. Looks correct — 생성 진행
- B. Request changes — 일부 수정

[Answer]: A. Looks correct

---

<!-- §13 Learnings Ritual — pending human turn (blank [Answer] marks genuine human-wait for the Stop hook) -->
## Learnings Ritual
프롬프트: "surface된 후보(c1~c3) 중 harness에 남길 항목을 고르고, 다음을 위해 추가할 메모가 있습니까?"
후보: c1(설계 전용 스코프에선 Bolt 계획=후속 구현 워크플로우 로드맵), c2(위상 허용 범위 내 경제적 Bolt 재배치는 위배 아님·rationale 명시), c3(walking-skeleton/수직 슬라이스엔 construction unit-major가 정합) — 각 `→ project.md`.
- 1. 아무것도 남기지 않음
- 2. 후보 선택 (남길 번호 지정; team 승격 여부)
- 3. 메모 추가 (자유 서술 + diary 헤딩 선택)

[Answer]: 1. 아무것도 남기지 않음
