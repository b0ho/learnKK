# Bolt Plan — learnKK (런크크)

<!-- delivery-planning 산출물(delivery 리드 + architect 검증). 출처: units-generation(unit-of-work/dependency/story-map), stories, requirements, mockups, components, team-practices. 이 스코프는 설계 전용(code-gen SKIP) — 이 Bolt 계획은 팀 3인의 후속 구현 워크플로우가 상속할 시퀀스 로드맵이다. walking-skeleton-first(team-practices) + 의존성/가치 하이브리드. -->

## 개요

- **시퀀싱:** walking-skeleton-first + 의존성/가치 하이브리드(Q1). 정량 WSJF 미사용, 서술적 근거(Q2, risk-and-sequencing-rationale.md).
- **세분화:** Bolt 1 = walking skeleton(U1+U2+U3 최소 관통), 이후 도메인 Unit당 1 Bolt(Q3).
- **병렬:** skeleton 선행 후 의존성 없는 Bolt는 3인 병렬(Q4).
- **주의:** 이번 워크플로우는 code-gen을 실행하지 않음(설계 전용). 이 계획은 후속 구현 워크플로우용 로드맵이며, 이 워크플로우의 construction 설계 단계(functional-design·nfr-requirements)는 **unit-major**로 Unit별 연속 설계한다(Q7).

## Bolt 시퀀스

### Bolt 1 — Walking Skeleton (공통 기반 관통) ★
- **포함 Unit:** U1 Contracts&Kernel + U2 Auth&Shell + U3 Meeting(최소 슬라이스: 개설→①승인→목록).
- **Walking skeleton:** 예. 관통 계층: 프론트(앱 셸·로그인·목록) ↔ REST(#1) ↔ 3계층(Controller/Service/Repository) ↔ PostgreSQL(Flyway 스키마) ↔ 도메인 타입(#3 상태 enum).
- **Definition of Done:** 3계약(#1/#2/#3) 선고정; 사번 가입·로그인·RBAC 동작; 모임 1건 개설→관리자 ①승인→목록 노출까지 end-to-end 통과; 테스트(80% coverage·계약 테스트) 통과.
- **확신 가설(무엇을 증명):** "선택한 아키텍처(모듈러 모놀리스+계약 우선+3계층)가 실제로 관통 동작하고, 3인이 이 계약 위에서 병렬 착수 가능하다"를 증명.
- **예상 데모:** 가입→로그인→모임 개설→관리자 승인→목록 확인.

### Bolt 2 — Meeting 완성 (U3 잔여)
- **포함 Unit:** U3 나머지(사전설문 문항 빌더·모집확정·②시작·③완료·반려/취소·운영 허브·상세).
- **DoD:** 모임 상태머신 전 전이 동작(①/모집확정/②/③·반려/취소), 문항 빌더, 멘토 운영 허브.
- **확신 가설:** 상태머신·승인 4지점(③=관리자 직접)이 불법 전이 방지(409)와 함께 정확히 동작.
- **데모:** 개설→승인→모집확정→시작→(세션 후)→관리자 완료 처리.

### Bolt 3 — Enrollment (U4)
- **포함 Unit:** U4.
- **DoD:** 선착순 신청·정원/중복 제어·취소·신청자 목록·멘티 현황.
- **확신 가설:** 잔여 1석 동시 신청·중복 신청 경계가 정원 무결성을 지킴.
- **데모:** 멘티 신청→현황 확인→취소.

### Bolt 4 — Content (U6)  |  Bolt 5 — Messaging (U7)  [Bolt 3 이후 병렬 가능]
- **B4 U6 DoD:** 게시글 본문+첨부(BLOB)·형식/크기 검증·공지. **가설:** 첨부 없는 글/첨부 포함 글 모두 저장·열람, 비참여자 403.
- **B5 U7 DoD:** 쪽지 스레드·미확인 뱃지·권한 경계. **가설:** 권한 경계(멘토=자기 모임 멘티, 관리자=전원)가 403으로 강제.

### Bolt 6 — Session/Attendance (U5)
- **포함 Unit:** U5.
- **DoD:** 세션 일정 지정·변경(복수)·멘티 팝업 출석(시간창)·출석율(세션 기준)·80% 자동판정·④ 확정 판정.
- **확신 가설:** 스케줄러리스 시간 판정이 세션창 개폐·출석 멱등·80% 경계(a*100≥80*S)를 정확히 처리.
- **데모:** 세션 생성→예정 시각 출석 팝업→체크→출석율/수료 판정.

### Bolt 7 — Survey/Feedback (U8)  [Bolt 3 이후 병렬 가능]
- **포함 Unit:** U8.
- **DoD:** 사전설문 응답(②후 게이팅)·과정설문·멘토/관리자 피드백 열람.
- **확신 가설:** 사전설문이 ②시작 이후에만 열리고, 피드백 열람 권한 경계가 지켜짐.

### Bolt 8 — Admin/Monitoring (U9)
- **포함 Unit:** U9.
- **DoD:** 승인 큐(①②③④+모집확정 대기) 조회·운영 현황 모니터링(세션 기준 출석율).
- **확신 가설:** 관리자 조회 계층이 각 도메인 read 조합으로 현황·큐를 정확히 집계(집계 지표는 범위 밖).
- **데모:** 관리자 대시보드에서 승인 큐 처리·현황 모니터링.

## 상위 흐름(요약)

Bolt 1(skeleton) → Bolt 2(Meeting 완성) → Bolt 3(Enrollment) → {Bolt 4 Content · Bolt 5 Messaging · Bolt 7 Survey} 병렬 → Bolt 6(Session/Attendance) → Bolt 8(Admin). (경제적 순서 근거는 risk-and-sequencing-rationale.md; 위상 제약은 units-generation DAG 준수.)

## Assumptions & Open Questions

- 이 계획은 후속 구현 워크플로우용 로드맵 — 실제 code-gen은 이번 스코프 밖.
- Bolt 6(U5)를 Content/Messaging보다 뒤에 둔 것은 의존(U4)+복잡도 분산 근거(rationale 참조). 팀은 구현 착수 시 재조정 가능.
- 세션 시간창·사번 형식·bytea/LO 등 미확정은 functional-design에서 확정.
