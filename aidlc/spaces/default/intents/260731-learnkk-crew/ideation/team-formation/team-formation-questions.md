# Team Formation — 질문지 (learnKK / 런크크)

3인 개발팀이 각자 배분된 단위(Bolt)를 개별 구현 워크플로우로 진행한다고 하셨습니다. 이 단계는 **각자의 강점, 작업 분배 방식, 협업 방식**을 정해 units-generation·delivery-planning의 배분 기준을 만듭니다. 스택은 React + Java Spring + PostgreSQL(feasibility 확정).

각 질문의 `[Answer]:` 뒤에 보기 문자를 적어주세요. 자유 서술은 `X. 기타`.

---

## Q1. 3명의 스킬 프로필은? (각자를 A/B/C로 표기해 주세요)
예: "1번=풀스택, 2번=백엔드, 3번=프론트" 처럼.
- A. 풀스택 (React + Spring 모두 가능)
- B. 백엔드 중심 (Java Spring)
- C. 프론트엔드 중심 (React)
- D. 다들 비슷한 풀스택 수준
- E. 아직 특정하기 어려움 — 균등 분배 가정
- X. 기타 (직접 서술: 예 "1=A, 2=B, 3=C")

[Answer]:d

## Q2. 작업 분배 방식 선호는?
- A. 기능 수직 슬라이스 — 각자 일부 proto-Unit을 프론트~백엔드까지 통째로 담당(풀스택 전제)
- B. 레이어 분담 — 프론트/백엔드/공통(DB·인증)으로 나눔
- C. 혼합 — 공통 기반(인증·모임 도메인)은 함께, 나머지는 수직 분담
- D. 추천대로 — 스킬 프로필에 맞춰 제안받고 싶음
- X. 기타 (직접 서술)

[Answer]:a

## Q3. 협업 방식은?
- A. 독립 병렬 — 각자 자기 Bolt를 독립적으로 구현(인터페이스만 합의)
- B. 페어(pair) — 2인 1조로 일부 진행
- C. 몹(mob) — 핵심 부분은 함께, 나머지 분담
- D. 추천대로
- X. 기타 (직접 서술)

[Answer]:a

## Q4. 위치·시간대·외부 인력 관련 특이사항이 있나요?
- A. 없음 — 동일 팀, 특이사항 없음
- B. 있음 (자유 서술: 시간대 분산, 외부 협력 등)
- X. 기타 (직접 서술)

[Answer]:a

---

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation
프롬프트: "정리된 답변이 맞습니까? 이대로 team-assessment / skill-matrix / mob-composition을 생성할까요?"
- A. 맞습니다 — 아티팩트 생성
- B. 수정 필요 — 일부 답변을 고치겠습니다

[Answer]: A. 맞습니다 — 아티팩트 생성
