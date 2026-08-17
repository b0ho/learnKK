# Tech Stack Decisions — U4 Enrollment (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U4 Enrollment(service). 출처: business-logic-model.md(선착순 정원), business-rules.md(BR-U4-1 동시성), requirements.md(C1·NFR8). U1 tech-stack 상속. U4는 정원 동시성 구현 기술 선택. -->

## 개요

U1 스택·계약 도구 상속(React+Spring+PostgreSQL, OpenAPI/Flyway/enum varchar+CHECK). U4는 선착순 정원 동시성 기술을 확정.

## U4 기술 선택

### TD-U4-1. 정원 동시성 — PostgreSQL 어드바이저리 락 (기본) / SERIALIZABLE (대안)

- **결정:** 모임 단위 신청 직렬화를 `pg_advisory_xact_lock(meetingId)`로 구현(기본). count-then-insert를 락 구간에서 원자 처리. 대안으로 SERIALIZABLE 격리 + 재시도.
- **근거:** 어드바이저리 락은 U3 meeting 테이블을 잠그지 않고(모듈 소유 준수) 모임 키로만 직렬화 — 서로 다른 모임 병렬. 단일/다중 인스턴스 모두 DB 수준 동작.
- **비채택:** U3 meeting 행 `FOR UPDATE`(모듈 소유 위반). U4 소유 카운터 행 락은 가능 변형.
- **Reversibility:** 중간(구현 세부, 무결성 계약은 불변).

### TD-U4-2. 중복 방지 — DB unique 제약

- **결정:** `unique(meeting_id, mentee_id)` — 애플리케이션 선검증 + DB 최종 보증. 위반→409.

### TD-U4-3. 현황 화면 조합 — FE 단일 API client

- **결정:** 멘티 현황은 백엔드 조인 아닌 FE 병렬 조합(U4/U5/U3). 백엔드 U4→U5 없음(순환 회피).

## 범위 밖

- 분산 락·메시지 큐(오버킬, 단일 인스턴스). CI/CD·운영(C3).

## Assumptions & Open Questions

- **[assumption]** 어드바이저리 락 기본, unique 중복 방지.
- **[open]** SERIALIZABLE 채택 시 재시도 정책, 다중 인스턴스 락 전략.
- **[open]** U3 정원 read 포트·U5 세션 read 시그니처(U3/U5 functional-design).
