# Feasibility & Constraints — 질문지 (learnKK / 런크크)

이 단계는 기술적 실현 가능성과 제약(기술·조직·규제)을 확인합니다. 이 프로젝트는 **로컬 전용 · 외부 서비스(AWS 등) 미사용 · 3인 개발 · 설계까지**로 이미 많은 제약이 정해져 있어, 관련 질문만 좁혀 드립니다.

각 질문의 `[Answer]:` 뒤에 보기 문자를 적어주세요. `(복수 선택 가능)` 표시가 있으면 여러 개 선택 가능. 해당 없으면 `N. 해당 없음`, 자유 서술은 `X. 기타`.

---

## Q1. 팀이 선호하거나 능숙한 기술 스택은? (설계 입력용, 복수 선택 가능)
구체 확정은 application-design에서 하지만, 방향을 알면 설계가 수월합니다.
- A. 웹 프론트 JS/TS 계열 (React/Vue 등) + Node 백엔드
- B. Python 백엔드 (FastAPI/Django/Flask 등)
- C. Java/Kotlin (Spring 등)
- D. 풀스택 프레임워크 하나로 통합 (예: Next.js, Django 등)
- E. 아직 미정 — 설계 단계에서 추천받고 싶음
- X. 기타 (직접 서술)

[Answer]:react + java 스프링

## Q2. 데이터 저장은 어떤 방향을 선호하나요? (로컬 실행 전제)
- A. 관계형 DB (PostgreSQL, MySQL 등)
- B. 경량 파일 DB (SQLite 등) — 로컬 개발에 간편
- C. 상관없음 — 설계 단계에서 추천받고 싶음
- X. 기타 (직접 서술)

[Answer]:a: postgresql

## Q3. 개인정보·보안 요구 수준은?
수집 정보: 닉네임, 비밀번호, 관심사 해시태그, 한 줄 소개.
- A. 최소 수준 — 비밀번호는 해시 저장, 그 외 특별한 규제 없음 (파일럿/로컬)
- B. 표준 수준 — 해시 저장 + 기본 세션/접근제어 + 최소한의 입력 검증
- C. 강화 필요 — 추가 보안 요구가 있음(자유 서술)
- N. 해당 없음
- X. 기타 (직접 서술)

[Answer]:a + 히든으로 ip 값 등을 통한 중복 계정 활용 방지만

## Q4. 연동해야 할 기존 시스템이나 외부 통합이 정말 없나요? (로컬 완결 확인)
- A. 없음 — 전부 로컬에서 자체 완결 (intent와 동일)
- B. 있음 — 연동 대상이 있음(자유 서술)
- X. 기타 (직접 서술)

[Answer]:a

## Q5. 일정·조직적 제약이 있나요? (복수 선택 가능)
- A. 특별한 마감/일정 제약 없음
- B. 설계 완료 목표 시점이 있음(자유 서술)
- C. 변경 동결·경쟁 우선순위 등 조직적 blocker 있음(자유 서술)
- N. 해당 없음
- X. 기타 (직접 서술)

[Answer]:a

---

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation
프롬프트: "정리된 답변이 맞습니까? 이대로 feasibility-assessment / constraint-register / raid-log를 생성할까요?"
- A. 맞습니다 — 아티팩트 생성
- B. 수정 필요 — 일부 답변을 고치겠습니다

[Answer]: A. 맞습니다 — 아티팩트 생성
