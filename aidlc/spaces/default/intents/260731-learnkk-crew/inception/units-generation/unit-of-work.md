# Unit of Work — learnKK (런크크)

<!-- units-generation 산출물(architect 리드 + delivery). 출처: application-design(components/component-methods/services/component-dependency/decisions), requirements, stories, team-practices. 도메인/수직 슬라이스 경계(Q1), 도메인당 1 Unit(Q2), 단일 배포(Q5), 3계약 통합(Q4). 위상만 — 빌드 순서·크리티컬 패스는 delivery-planning 소관. -->

## 개요

- **경계 전략:** 도메인/수직 슬라이스 — 각 Unit이 백엔드 모듈 + 관련 프론트 화면을 함께 소유(team-practices 수직 슬라이스). 공통 커널·3계약·앱 셸은 Foundation Unit(U1/U2)으로 분리.
- **배포:** 단일 배포(모듈러 모놀리스, application-design ADR-001). Unit은 코드·소유 경계이지 배포 경계가 아니다.
- **통합 계약:** #1 OpenAPI(REST 경계), #2 DB 스키마(모듈 테이블 소유), #3 도메인 타입(C0 상태/enum) — U1이 소유·선고정, 나머지 Unit이 계약에 맞춰 병렬 구현.
- **complexity:** S/M/L/XL 상대 추정(빌드 순서 아님).

## Unit 정의

### U1. Contracts & Kernel
- **kind:** `spec`
- **소유/전달:** C0 Shared Kernel(모임 상태 enum·수료 상태 enum·에러 스키마 `{code,message,details}`·사번 등 값객체·RBAC 역할) + 3계약 스캐폴딩(#1 OpenAPI 스펙, #2 DB baseline + Flyway 초기 마이그레이션, #3 도메인 타입 용어집).
- **배포 모델:** embedded(공유 라이브러리/스펙, 런타임 없음).
- **complexity:** M — 계약 정의·합의가 핵심.
- **노트:** walking skeleton의 계약 기반. 이 Unit이 선고정되어야 나머지가 병렬 착수 가능(team-practices). application-design ADR-006(상태머신 소유), ADR-003(Flyway).

### U2. Auth & App Shell
- **kind:** `service`
- **소유/전달:** C1(사번 포함 회원가입·로그인·세션·RBAC 경계·프로필) + 프론트 앱 셸(3탭 네비·라우팅·shadcn 공통 UI 셋업·단일 API client·로그인/가입/내정보 화면).
- **배포 모델:** shared(모놀리스에 통합; FE 셸은 SPA 기반).
- **complexity:** L — 인증·세션·RBAC + FE 기반 셸.
- **노트:** walking skeleton 본체(인증 + 앱 셸). API 계약 #1의 인증/인가 엔드포인트를 핀(application-design US-1.2). 사번 필드 맨 위(rev4).

### U3. Meeting
- **kind:** `service`
- **소유/전달:** C2(모임 개설·사전설문 문항 빌더·상태머신 전이 집행 ①개설/모집확정/②시작/③완료·반려/취소) + 멘토 개설 화면·운영 허브·모임 목록/상세.
- **배포 모델:** shared.
- **complexity:** XL — 상태머신·승인 4지점 다수·문항 빌더·다수 화면.
- **노트:** 상태머신 전이 단일 소유(ADR-006). ③완료는 관리자 직접(rev3), 전제조건은 U5 세션 종료 read(ADR-007 R-2). 모집확정 US-3.4.

### U4. Enrollment
- **kind:** `service`
- **소유/전달:** C3(선착순 신청·정원/중복 제어·신청 취소·신청자 목록) + 멘티 신청·내 현황(내 신청/모임) 화면.
- **배포 모델:** shared.
- **complexity:** M.
- **노트:** 동시성(잔여 1석)·중복 신청 경계(stories US-3.2). 모임 상태·정원은 U3 read(ADR-007 R-1).

### U5. Session/Attendance
- **kind:** `service`
- **소유/전달:** C4(멘토 세션 일정 지정·변경·복수 세션·멘티 팝업 출석 self check-in·출석율 세션 기준·80% 수료 자동판정·④ 관리자 확정 판정 로직) + 세션 관리·출석 팝업·멘티 현황 화면.
- **배포 모델:** shared.
- **complexity:** L — 시간창 판정·세션별 출석·수료 계산.
- **노트:** 스케줄러리스 시간 판정(ADR-005). 분모=전체 예정 세션(a*100≥80*S). ②시작 이후 활성.

### U6. Content
- **kind:** `service`
- **소유/전달:** C5(주차 게시글 본문+선택 첨부·BLOB 저장/형식·크기 검증·공지) + 자료실·공지 화면.
- **배포 모델:** shared.
- **complexity:** M — BLOB 스트리밍·형식 검증.
- **노트:** 첨부 BLOB(ADR-004, bytea/LO는 functional-design). 참여자 열람 권한은 U4 read.

### U7. Messaging
- **kind:** `service`
- **소유/전달:** C6(쪽지 스레드·인앱 미확인 뱃지 폴링·권한 경계) + 쪽지 화면.
- **배포 모델:** shared.
- **complexity:** M.
- **노트:** 채팅형 전환 미확정(OQ2). 멘토-멘티 관계 권한은 U3/U4 read.

### U8. Survey/Feedback
- **kind:** `service`
- **소유/전달:** C7(사전설문 응답 ②시작 후 수집·열람·과정 설문 제출·멘토/관리자 피드백 열람) + 설문 응답·피드백 화면.
- **배포 모델:** shared.
- **complexity:** M.
- **노트:** 사전설문 응답은 ②후(rev2, US-3.6). 문항 틀은 U3 소유(read), 응답은 U8 소유.

### U9. Admin/Monitoring
- **kind:** `service` (read/조회 계층)
- **소유/전달:** C8(승인 큐 집계 ①②③④+모집확정 대기·운영 현황 모니터링) + 관리자 모니터링 화면.
- **배포 모델:** shared.
- **complexity:** M — 다수 도메인 read 조합.
- **노트:** 실제 승인 액션은 U3/U5 Service. 여기는 read 조합(ADR-007). 집계 지표는 범위 밖(US-9.3 Won't).

## Unit 요약표

| Unit | kind | complexity | depends_on |
|------|------|-----------|------------|
| U1 Contracts&Kernel | spec | M | — |
| U2 Auth & App Shell | service | L | U1 |
| U3 Meeting | service | XL | U1,U2 |
| U4 Enrollment | service | M | U1,U2,U3 |
| U5 Session/Attendance | service | L | U1,U2,U3,U4 |
| U6 Content | service | M | U1,U2,U3 |
| U7 Messaging | service | M | U1,U2,U3 |
| U8 Survey/Feedback | service | M | U1,U2,U3,U4 |
| U9 Admin/Monitoring | service | M | U1,U2,U3,U4,U5,U8 |

## 구현 노트·제약 (per unit)

- 전 Unit: team-practices 상속(3계층·Entity 비노출·DTO 경계·JSON camelCase/JPA snake_case·테스트 80% coverage·계약 테스트). 사용자 노출 텍스트 한국어.
- U1 계약 선고정이 병렬 착수의 하드 선행(walking skeleton).
- read 상호참조(U3↔U4, U3↔U5)는 read 포트/컨트롤러 조합(ADR-007) — functional-design 확정.
- 미확정(하류): 사번 형식(A5)·출석 유효 시간창·세션 변경 통지(A6)·사전설문 미응답(OQ7)·bytea/LO·반려 사유 UI·시작대기 취소(US-3.3).

## Assumptions & Open Questions

- 3인 소유 배분·빌드 순서·MVP 경계는 delivery-planning(2.8) 확정. 이 문서는 위상만.
- Unit kind는 construction 설계 아티팩트 매트릭스를 좌우 — U1(spec)은 스케일러빌리티 문서 등 불요, service Unit은 전체 적용.
## Review

**Reviewer:** aidlc-architecture-reviewer-agent

Verdict: READY

Adversarial review of the Unit-of-Work decomposition (unit-of-work.md, unit-of-work-dependency.md, unit-of-work-story-map.md) against application-design (components C0–C8, component-methods, services, component-dependency, decisions ADR-001..007), requirements, and stories. I tried to break the DAG, the coverage, and the scope boundary — and failed to reach a blocking finding.

### Blocking (none)

None.

### Verification evidence (what I checked)

- **DAG validity — PASS.** The fenced `yaml` edge block in unit-of-work-dependency.md is present, well-formed, and names 9 units exactly once (U1-contracts-kernel, U2-auth-shell, U3-meeting, U4-enrollment, U5-session-attendance, U6-content, U7-messaging, U8-survey-feedback, U9-admin-monitoring). Every `depends_on` target is a declared unit; no self-dependency; every `kind` ∈ {spec, service}. Independent topological sort peels cleanly — U1 → U2 → U3 → {U4, U6, U7} → {U5, U8} → U9 — so the graph is acyclic. The edge block matches the unit-of-work.md summary table `depends_on` column and the dependency-doc tree/text fallback (tree is labelled an approximation; edge block declared canonical).
- **Scope boundary — PASS.** No "recommended build order", "critical path", or "build first" economic-sequencing language. Sequential statements (U1→U2, "U9 후행") are dependency facts, and S/M/L/XL complexity is explicitly "빌드 순서 아님". Parallel-opportunity prose is present and allowed. All three artifacts repeat the disclaimer that build order / critical path / Bolt batching belong to delivery-planning (2.8). The within-unit story order in the story-map is explicitly labelled "빌드 배치 아님" and is a Step-6 required item, not a violation. The U1+U2 "walking skeleton foundation" framing is topological (U1 is the zero-dep root; everything depends on U1 and U2), not economic.
- **Coverage — PASS.** All 30 Must stories (US-1.1..US-9.2) map to a unit; US-9.3 is correctly Won't (not unmapped). Every unit U2–U9 carries ≥1 story; U1 (spec) legitimately owns contracts #1/#2/#3 and domain types rather than stories. No orphan story, no story-less domain unit. Cross-cutting admin approvals are split correctly: action-owner = U3 (state transitions ①/모집확정/②/③) and U5 (④ 판정/확정), queue view = U9 — for US-2.2, US-3.4, US-6.1, US-7.2, US-7.3.
- **Consistency with application-design — PASS.** Clean 1:1 unit↔component mapping (U1←C0+contracts, U2←C1, U3←C2, U4←C3, U5←C4, U6←C5, U7←C6, U8←C7, U9←C8). Single-deploy modular monolith honored (ADR-001; units are code/ownership, not deploy, boundaries). Contracts #1/#2/#3 owned by U1. The read cross-references (U3↔U4 applicant/capacity, U3↔U5 session-completion precondition for ③) are modelled per ADR-007 as read-port / controller composition with writes single-owned, so no write cycle enters the edge block.
- **Unit kind correctness — PASS.** U1=spec (embedded, no runtime); domain units=service; vertical-slice UI folded into each domain unit with the common app shell in U2 (Q6=A) — coherent with the chosen boundary strategy.
- **team-practices alignment — PASS.** Walking-skeleton foundation (U1 contracts + U2 auth/shell) precedes all domain units in the DAG (U3–U9 all depend on U1 and U2). 3-dev parallel ownership is feasible (U3 then U4/U6/U7 independent); candidate assignment deferred to delivery-planning.
- **Sensors — PASS.** required-sections: each artifact has ≥2 H2 headings (unit-of-work 5, dependency 6, story-map 5) and the dependency file's yaml edge block is present/well-formed/cycle-free. upstream-coverage: unit-of-work.md prose references components, component-methods, services, component-dependency, decisions, requirements, and stories.

### Suggestions (non-blocking; downstream)

- **S1 — Pin the read-port interfaces in U1 to preserve acyclicity (functional-design).** The edge block keeps U3 at `depends_on: [U1, U2]` even though U3 hosts US-2.3 (reads U4 applicants, U8 responses) and US-7.3 ③완료 (reads U5 "전 세션 종료"). Acyclicity holds only if those read ports are owned by U1's contracts (#1/#3) so U3 compiles against U1, not against U4/U5. If functional-design instead places a direct U3→U4/U3→U5 read dependency, the DAG silently gains back-edges opposite U4→U3 / U5→U3 and risks a cycle. Recommend an explicit note that these read ports belong to U1. (ADR-007 is Status: Proposed — this is the item it must resolve.)
- **S2 — Delivery timing of the mentor-hub read integration.** US-2.3 (in U3) depends story-wise on US-3.2 (U4) and US-3.6 (U8), i.e. opposite the unit DAG. Correctly documented as read composition; delivery-planning should note that U3's mentor-hub story only fully lands once U4/U8 read integration is available. Not a decomposition defect.
- **S3 — upstream-coverage robustness.** components / component-methods / services / component-dependency appear chiefly in the header HTML comment; surfacing one or two in visible prose would keep the sensor robust to comment-stripping.
- **S4 — Naming cross-walk.** The edge block uses slug names (e.g. `U1-contracts-kernel`) while the body, summary table, and story-map use short IDs (U1). Harmless, but a one-line mapping note would tighten the machine/human cross-walk.
