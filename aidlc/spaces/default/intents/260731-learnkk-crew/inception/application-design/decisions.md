# Architecture Decision Records — learnKK (런크크)

<!-- application-design 산출물. ADR 형식은 adr-template. 출처: application-design 답변(Q1~Q7), requirements(NFR), team-practices, stories. 설계 전용 — Status는 Accepted(설계 확정), 구현은 후속. -->

## ADR Index

| ADR | Title | Status | Reversibility | Date |
|-----|-------|--------|---------------|------|
| 001 | 모듈러 모놀리스 채택 | Accepted | 중간(모듈→서비스 분리 가능) | 2026-08-17 |
| 002 | 동기 REST + 인-프로세스 모듈 호출 | Accepted | 중간(이벤트 도입 여지) | 2026-08-17 |
| 003 | 단일 PostgreSQL + 모듈별 테이블 소유 (Flyway) | Accepted | 낮음(스키마·마이그레이션 락인) | 2026-08-17 |
| 004 | 첨부 파일 PostgreSQL BLOB 저장 | Accepted | 중간(외부 스토리지로 이관 가능) | 2026-08-17 |
| 005 | 스케줄러리스 세션·출석 시간 판정 | Accepted | 높음(배치 추가 용이) | 2026-08-17 |
| 006 | 모임 상태머신 소유 위치 (C0 enum + C2 전이) | Accepted | 낮음(계약 #3, 광범위 참조) | 2026-08-17 |
| 007 | Meeting↔(Enrollment, Session) read 상호참조 정리 | Proposed | 높음(read 포트 배치 조정 용이) | 2026-08-17 |

---

## ADR-001: 모듈러 모놀리스 채택

### Status
Accepted

### Date
2026-08-17

### Context
전부 로컬·파일럿 규모(동시 수십 명, 모임 수십 개 — NFR2), 3인 독립 병렬 개발(team-practices), 외부 SaaS/클라우드 미사용(C2). 아키텍처 스타일을 정해야 한다.

### Decision
단일 Spring Boot 앱 내에서 도메인 모듈(패키지 경계, C1~C8)로 분리하는 **모듈러 모놀리스**를 채택한다.

### Consequences
- **Positive:** 로컬 단일 인스턴스에 최적, 배포·운영 단순, 인-프로세스 호출로 지연 최소. 모듈 경계로 3인 병렬 소유 배분 가능. 계약(#1/#2/#3)으로 모듈 인터페이스 고정.
- **Negative:** 모듈 경계가 코드 규율에 의존(물리 분리 아님) — 경계 침범 위험은 team-practices(Entity 비노출·Service 경유)로 통제.
- **Neutral:** 향후 특정 모듈을 서비스로 분리할 여지는 남되 현재 불필요.

### Alternatives Considered
- **마이크로서비스:** 다중 프로세스·네트워크 통신. 로컬·소규모엔 운영 복잡도 과다 → 기각.
- **단순 계층형(모듈 경계 약함):** 빠르나 3인 병렬 소유·interface 계약이 흐려져 병합 충돌 위험 → 기각.

### References
Q1, team-practices(monorepo·3계층), requirements NFR2/NFR4.

---

## ADR-002: 동기 REST + 인-프로세스 모듈 호출

### Status
Accepted

### Date
2026-08-17

### Context
클라이언트(React)↔백엔드, 백엔드 모듈 간 통신 방식을 정해야 한다. 로컬 단일 인스턴스.

### Decision
클라이언트↔백엔드는 **동기 REST(JSON, OpenAPI #1)**, 백엔드 모듈 간은 **인-프로세스 동기 Service 인터페이스 호출**. 메시지 브로커·이벤트버스·비동기 큐는 두지 않는다.

### Consequences
- **Positive:** 단순·디버깅 용이, 지연 최소, 인프라 부담 없음(전부 로컬). 계약 테스트(team-practices)가 REST 계약을 검증.
- **Negative:** 강결합 가능성 — Service 인터페이스·DTO 경계로 완화. 장기적 확장 시 이벤트 도입 필요할 수 있음.
- **Neutral:** 오케스트레이션(호출 측 조율) 방식 채택(choreography 아님).

### Alternatives Considered
- **인메모리 도메인 이벤트:** 모듈 결합 완화하나 소규모엔 오버엔지니어링 → 기각(향후 여지).
- **비동기 큐/브로커:** 전부 로컬·소규모 범위 밖 → 기각.

### References
Q3, services.md 통신 계약.

---

## ADR-003: 단일 PostgreSQL + 모듈별 테이블 소유 (Flyway)

### Status
Accepted

### Date
2026-08-17

### Context
데이터 저장 전략. 스택은 PostgreSQL 고정(team-practices). 모듈 경계와 데이터 소유를 정렬해야 interface 불일치를 막는다.

### Decision
**단일 PostgreSQL**을 쓰되 도메인 모듈이 **자기 테이블만 소유**하고, 교차 접근은 소유 모듈 Service를 경유한다(직접 테이블 접근 금지). 마이그레이션은 **Flyway**로 관리한다(DB 스키마 계약 #2).

### Consequences
- **Positive:** 단일 DB로 트랜잭션·조인 단순, 로컬 적합. 모듈 소유 경계로 병렬 개발 충돌 감소. Flyway로 스키마 버전·계약 고정.
- **Negative:** 물리적으로 한 DB라 소유 경계는 규율 의존(외래키 교차 가능) — 리뷰·규약으로 통제.
- **Neutral:** 스키마 계약(#2) 소유자·초기 마이그레이션은 delivery-planning/구현에서 배정.

### Alternatives Considered
- **공유 접근(소유 경계 약함):** 빠르나 결합·충돌 → 기각.
- **모듈별 분리 DB:** 분산 트랜잭션 복잡, 범위 밖 → 기각.
- **마이그레이션 Liquibase:** 유효 대안이나 팀이 Flyway 선택(Q4) → Flyway 채택.

### References
Q4(+Flyway), team-practices 계약 #2, requirements C1.

---

## ADR-004: 첨부 파일 PostgreSQL BLOB 저장

### Status
Accepted

### Date
2026-08-17

### Context
자료실 게시글의 첨부 파일(문서 위주, 파일당 상한) 저장 위치. 전부 로컬·외부 오브젝트 스토리지(S3 등) 미사용(C2). stories US-4.1b는 BLOB 직접 저장 확정.

### Decision
첨부 파일을 **PostgreSQL**에 직접 저장하고 메타데이터(파일명·형식·크기·업로더·소속 게시글/주차)를 함께 관리한다. 저장 타입은 파일당 상한(제안 20MB)에서 `bytea`로 충분하나, **진정한 스트리밍이 필요하면 PostgreSQL Large Object(LO) API**를 쓴다 — bytea는 통상 전량 메모리 적재되므로 저장 타입 최종 선택(bytea vs LO)은 functional-design/구현에서 상한·스트리밍 요구와 함께 확정한다.

### Consequences
- **Positive:** 외부 스토리지 의존 없이 전부 로컬 충족. 백업·트랜잭션 일관성이 DB와 단일.
- **Negative:** 대용량·다수 첨부 시 DB 부하·메모리 위험 → 스트리밍 + 파일당 상한(기본 제안 20MB, 최종 OQ4)으로 완화. 파일럿 규모라 허용.
- **Neutral:** 형식 화이트리스트·최종 상한은 functional-design(OQ4).

### Alternatives Considered
- **로컬 파일시스템 경로 + DB 메타:** 대안이나 stories Q4=B(BLOB 직접) 확정 → 채택 안 함.
- **외부 오브젝트 스토리지:** C2(전부 로컬) 위반 → 기각.

### References
stories US-4.1b(Q4=B), requirements FR4.3/4.4/OQ4.

---

## ADR-005: 스케줄러리스 세션·출석 시간 판정

### Status
Accepted

### Date
2026-08-17

### Context
rev2로 멘토가 세션 날짜·시간을 지정하고(주차당 복수), 예정 시간에 멘티가 팝업으로 출석 체크한다. 시간 기반 로직의 구현 방식을 정해야 한다. 로컬 단일 인스턴스.

### Decision
**배치/스케줄러 없이**, 요청 시점에 현재 시각과 세션 예정 시각(및 유효 시간창)을 비교해 출석 창 유효성·세션 상태를 판정한다.

### Consequences
- **Positive:** cron/배치 인프라 불필요, 로컬 단일 인스턴스에 정합, 단순. 세션 일정 변경(유동)에도 판정이 항상 최신.
- **Negative:** "자동으로 상태가 바뀌는" 뷰가 필요하면 조회 시 계산해야 함 — 파일럿 규모라 부담 없음.
- **Neutral:** 출석 유효 시간창 파라미터(세션 시각 기준 허용 범위)는 functional-design 확정.

### Alternatives Considered
- **앱 내 @Scheduled 배치:** 세션 상태를 주기 갱신. 단일 인스턴스·소규모엔 불필요한 복잡도 → 기각(필요 시 후속 도입).

### References
Q5, stories US-6.2/6.3, developer 기고.

---

## ADR-006: 모임 상태머신 소유 위치 (C0 enum + C2 전이)

### Status
Accepted

### Date
2026-08-17

### Context
모임 상태머신(개설신청→모집중→시작대기→진행중→완료 + 반려/취소)과 멘티 수료 상태는 여러 모듈이 참조·전이시킨다(①C2, 모집확정 C2, ②C2, ③C2 rev3, ④C4/C2, 신청 C3). 독립 병렬의 최대 리스크는 interface 불일치(team-practices 계약 #3).

### Decision
상태·수료 **enum 정의는 Shared Kernel(C0)** 에 두고(단일 소유), **전이 규칙의 집행은 Meeting(C2)** 이 단일 소유한다. 불법·이중·역순 전이는 409로 거부(stories CC-1). 이는 도메인 타입 계약 #3의 물리적 실현이다.

### Consequences
- **Positive:** enum 중복·불일치 제거, 전이 로직 단일화로 상태 무결성 보장. 3인 병렬에서 상태 관련 계약이 한 곳.
- **Negative:** C2가 상태 전이의 병목·핵심 소유 — 초기 계약 고정이 병렬 착수 전 선행되어야 함(walking skeleton).
- **Neutral:** ④멘티수료 확정은 관리자 액션이나 판정 로직은 C4(Completion) — C2/C4 협력은 오케스트레이션.

### Alternatives Considered
- **각 모듈이 상태 enum 자체 정의:** interface 불일치 1순위 원인 → 기각.
- **상태 전이를 각 액션 모듈에 분산:** 무결성 규칙 산재·검증 곤란 → 기각.

### References
team-practices 계약 #3, stories 상태머신 섹션, components.md C0/C2.

---

## ADR-007: Meeting↔(Enrollment, Session) read 상호참조 정리

### Status
Proposed

### Date
2026-08-17

### Context
두 곳에서 Meeting(C2)과 read 상호참조가 발생한다(component-dependency R-1/R-2):
- **C2↔C3**: 멘토 운영 허브(C2, US-2.3)가 신청자 목록(C3)을 read; 신청(C3)이 모임 상태·정원(C2)을 read.
- **C2↔C4**: ③완료(US-7.3)의 전제조건 "전 세션 종료"는 세션 소유자 C4의 상태 → 완료 처리 흐름이 C4를 read; 기존 C4→C2(진행중 상태 read)와 합쳐 상호참조.

두 경우 모두 쓰기 전이는 단일 소유(C2/C4)라 **쓰기 순환은 없으나** 컴파일-레벨 read 순환 위험이 있다.

### Decision
쓰기 순환 없음을 확인. read 상호참조는 다음 중 하나로 정리한다(물리 배치는 functional-design/units-generation 확정):
- (a) **프론트 단일 API client가 컨트롤러/클라이언트 레벨에서 조합**(예: 멘토 허브가 C2·C3 응답을 각각 받아 합침) — 백엔드 서비스 순환 자체 제거,
- (b) **공유 read 포트(인터페이스)** 로 read 의존을 분리(C2가 C4/C3 read 포트에 의존, 구현체는 소유 모듈),
- (c) 조회 조합은 **C8(Admin/조회 계층)** 활용.
③완료 전제조건(C2↔C4)은 특히 **관리자 완료 오케스트레이션이 C4 확인 후 completeMeeting 호출**하는 (a)/(b) 혼합이 유력.

### Consequences
- **Positive:** 순환 의존 방지, 모듈 컴파일 독립성 유지.
- **Negative:** read 포트 추상화가 약간의 보일러플레이트 추가.
- **Neutral:** 최종 배치(포트 위치)는 하류 결정 — 그래서 Status=Proposed.

### Alternatives Considered
- **양방향 직접 의존 허용:** 순환 → 기각.
- **모든 조회를 C8로 강제:** 단순하나 C8이 비대해질 수 있음 → 부분 적용 검토.

### References
component-dependency.md 순환 검증(R-1 C2↔C3, R-2 C2↔C4), components.md C2/C3/C4/C8, component-methods.md completeMeeting.

## Assumptions & Open Questions

- ADR-007은 Proposed — functional-design/units-generation에서 확정.
- 세션 저장 방식(서버 세션 vs JWT), 반려 사유 필수 여부, 사번 형식은 functional-design(requirements A5/A6/OQ7).
