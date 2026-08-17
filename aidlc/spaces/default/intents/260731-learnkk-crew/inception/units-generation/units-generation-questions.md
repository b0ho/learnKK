# Units Generation — 계획 질문지 (learnKK / 런크크)

application-design(C0~C8 모듈·의존)·stories·team-practices(3인 수직 슬라이스·walking skeleton·3계약)를 전제로 Unit of Work **위상(DAG)** 을 정합니다. (빌드 순서·크리티컬 패스는 이 단계가 아니라 delivery-planning 소관.)

각 `[Answer]:`에 보기 문자. 복수 선택 문항 표시. 직접 서술은 `X. 기타`.

---

## Q1. Unit 경계 전략
- A. (권장) **도메인/수직 슬라이스 기반** — application-design 컴포넌트(C1~C8)를 도메인 Unit으로 매핑하되 각 Unit은 백엔드+관련 프론트 화면을 포함(team-practices 수직 슬라이스). 공통 기반(C0 커널 + 3계약 + 인증)은 **Foundation Unit**으로 분리.
- B. 계층(layer) 기반 — 백엔드 전체 / 프론트 전체로 분리
- C. 서비스 배포단위 기반 — 이번은 단일 배포라 부적합
- X. 기타

[Answer]:a

## Q2. Unit 세분화(granularity)
- A. (권장) **도메인당 1 Unit**(약 9~10개: Foundation + 7 도메인 + Admin조회 + Frontend-shell). 3인 병렬 소유에 적합.
- B. 더 굵게(3~4개로 통합, 3인에 1~2개씩)
- C. 더 잘게(도메인을 화면/기능 단위로 분할)
- X. 기타

[Answer]:a

## Q3. 의존 순서/병렬성 표현
- A. (권장) **엄격 위상(DAG) + 독립 Unit 병렬 허용** — 의존 없는 Unit들은 병렬 개발 가능함을 명시(순서 강제 아님). 실제 순서는 delivery-planning.
- B. 엄격 위상만(병렬 기회 표기 안 함)
- X. 기타

[Answer]:a

## Q4. Unit 간 통합점/계약
- A. (권장) 통합은 **3계약으로 고정**: #1 OpenAPI(REST 경계), #2 DB 스키마(모듈 테이블 소유), #3 도메인 타입(C0 상태/enum). Foundation Unit이 이 계약을 소유·선고정 → 나머지 Unit이 계약에 맞춰 병렬 구현.
- B. 계약 없이 Unit 간 직접 조율
- X. 기타

[Answer]:a

## Q5. 배포 모델
- A. (권장) **단일 배포(모듈러 모놀리스)** — 모든 백엔드 Unit은 한 Spring Boot 앱에 통합, 프론트는 정적 SPA. 로컬 단일 인스턴스(application-design ADR-001). Unit은 코드/소유 경계이지 배포 경계가 아님.
- B. Unit별 독립 배포 — 범위 밖
- X. 기타

[Answer]:a

## Q6. Frontend Unit 처리
- A. (권장) 각 도메인 Unit이 **자기 화면(수직 슬라이스)을 포함**하고, 공통 앱 셸(3탭 네비·라우팅·공통 UI·API client)은 **Foundation/Shell Unit**에 둔다.
- B. 프론트 전체를 별도 단일 Unit으로 분리
- X. 기타

[Answer]:a

---

<!-- Decomposition Plan Approval (Step 5) -->
## Decomposition Plan (제안)

경계 전략=도메인/수직 슬라이스 + Foundation 분리(Q1=A), 도메인당 1 Unit(Q2=A), 단일 배포(Q5=A), 통합=3계약(Q4=A), Frontend는 각 도메인 슬라이스에 포함·공통 셸은 Foundation(Q6=A).

제안 Unit 9개 (name · kind · 소유 컴포넌트):
- **U1 Contracts&Kernel** · `spec` · C0 + #1 OpenAPI + #2 DB baseline/Flyway + #3 도메인 타입/enum/에러/RBAC. depends: []
- **U2 Auth & App Shell** · `service` · C1(사번 가입·로그인·세션·RBAC·프로필) + FE 셸(3탭 네비·라우팅·shadcn 공통 UI·단일 API client). depends: [U1] — walking skeleton 기반
- **U3 Meeting** · `service` · C2(개설·설문 문항 빌더·상태머신 전이 ①/모집확정/②/③·반려/취소) + 멘토 개설/운영허브·모임 목록/상세 화면. depends: [U1,U2]
- **U4 Enrollment** · `service` · C3(선착순 신청·취소·신청자) + 멘티 신청/현황 화면. depends: [U1,U2,U3]
- **U5 Session/Attendance** · `service` · C4(세션 일정·팝업 출석·출석율·80% 자동판정·④) + 세션관리/출석팝업/멘티현황. depends: [U1,U2,U3,U4]
- **U6 Content** · `service` · C5(게시글·첨부 BLOB·공지) + 자료실 화면. depends: [U1,U2,U3]
- **U7 Messaging** · `service` · C6(쪽지) + 쪽지 화면. depends: [U1,U2,U3]
- **U8 Survey/Feedback** · `service` · C7(사전설문 응답 ②후·과정설문·피드백 열람) + 설문/피드백 화면. depends: [U1,U2,U3,U4]
- **U9 Admin/Monitoring** · `service`(read 계층) · C8(승인 큐·현황 조회) + 관리자 모니터링 화면. depends: [U1,U2,U3,U4,U5,U8]

병렬 기회(Q3=A): U1→U2 후, U3 완료 시 U4/U6/U7 병렬 가능. (실제 빌드 순서·크리티컬 패스는 delivery-planning.)

프롬프트: "이 분해 계획으로 Unit 산출물을 생성할까요?"
- A. Approve Plan — 이 계획으로 생성
- B. Revise Plan — 계획 수정

[Answer]: A. Approve Plan

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->

---

<!-- §13 Learnings Ritual — pending human turn (blank [Answer] marks genuine human-wait for the Stop hook) -->
## Learnings Ritual
프롬프트: "surface된 후보(c1~c2) 중 harness에 남길 항목을 고르고, 다음을 위해 추가할 메모가 있습니까?"
후보: c1(units는 위상만, 빌드 순서는 delivery-planning 소관), c2(read 상호참조는 edge block 대신 U1 계약 read 포트로 흡수해 DAG 비순환 유지) — 각 `→ project.md`.
- 1. 아무것도 남기지 않음
- 2. 후보 선택 (남길 번호 지정; team 승격 여부)
- 3. 메모 추가 (자유 서술 + diary 헤딩 선택)

[Answer]: 1. 아무것도 남기지 않음
