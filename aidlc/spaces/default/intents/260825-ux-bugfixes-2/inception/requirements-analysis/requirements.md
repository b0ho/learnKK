# 요구사항 — learnKK UX/동작 버그픽스 2차 (ux-bugfixes-2)

## Intent 분석
learnKK(브라운필드; `business-overview.md`·`architecture.md`·`code-structure.md` 참조)를 멘토/멘티/관리자로 실사용하며 발견된 11건의 UX·동작 결함을 수정한다. 1차 버그픽스(ux-bugfixes)와 디자인 시스템 적용 이후의 후속 보정으로, 신규 도메인 추가가 아니라 기존 U3~U9 동작·내비게이션·시드 데이터의 개선이다. 목표는 내비게이션 일관성(탭/뒤로가기), 내 러닝 진입 동선, 설문/출석 표시 정확성, 관리자 승인 운영성 정리, 그리고 데모용 초기 데이터 확충이다.

## 기능 요구사항 (FR)

### FR-1 활성 탭 재클릭 시 새로고침 — Must
하단 내비게이션에서 현재 위치한 탭을 다시 누르면 해당 탭이 루트 화면으로 리셋되고 데이터가 재조회된다.
- Given `/meetings`(모임 탭)에 있고 상세 등 하위로 이동한 상태, When 하단 '모임' 탭을 다시 클릭, Then 모임 목록 루트로 이동하며 목록을 재조회한다.
- Given 이미 탭 루트에 있음, When 같은 탭 재클릭, Then 데이터가 재조회(reload)된다(무반응 no-op 아님).

### FR-2 자료실·피드백 진입 시 '내 러닝' 탭 컨텍스트 유지 — Must
'내 러닝'에서 공지/자료실·피드백으로 진입할 때, 하단 내비의 활성 탭이 '모임'으로 전환되지 않고 '내 러닝'에 머물러야 한다.
- **현상(버그)**: 현재 자료실·피드백 라우트가 `/meetings/{id}/...` 하위라, '내 러닝'에서 진입해도 하단 탭이 '모임'으로 하이라이트된다.
- Given 멘티/멘토가 '내 러닝' 카드에서 공지/자료실 또는 피드백 진입, Then 화면은 해당 상세로 이동하되 하단 내비의 활성 탭은 '내 러닝'으로 유지된다.
- **구현 방향**: 자료실·피드백(멘티 제출/멘토·관리자 열람)을 '내 러닝' 하위 경로(`/my-learning/...`)로도 진입 가능하게 하여(동일 페이지 컴포넌트 재사용) '내 러닝' NavLink가 활성 상태를 유지한다. '내 러닝' 카드의 진입점은 이 my-learning 스코프 경로로 이동한다.
- FR-3(이전 버튼)과 결합: 진입 후 우하단 '이전'으로 '내 러닝'으로 복귀한다.

### FR-3 뎁스 이동 시 이전화면 버튼(우하단) — Must
탭 내에서 하위(뎁스) 화면으로 이동했을 때, 이전 화면으로 돌아가는 버튼을 화면 우하단에 플로팅으로 제공한다.
- Given 탭 루트가 아닌 하위 화면(모임 상세/자료실/스레드/설문/피드백 등), Then 우하단에 '이전' 버튼이 표시된다.
- When 버튼 클릭, Then 브라우저 히스토리 상 직전 화면으로 이동한다(`navigate(-1)`).
- 탭 루트(목록) 화면에서는 표시하지 않는다.

### FR-4 사전설문 응답 보기에 문항 함께 표시 — Must
사전설문 응답 열람 화면에서 각 응답을 해당 문항(질문 텍스트)과 함께 표시한다.
- Given 소유 멘토·관리자·본인이 사전설문 응답을 조회, Then 각 응답 항목이 "문항 텍스트 + 응답 값" 형태로 렌더되어 어떤 질문에 대한 답인지 식별된다.
- 문항 정보는 기존 문항 조회 API(`GET /api/meetings/{id}/questions`)로 결합한다.

### FR-5 출석 완료 상태 유지 표시 — Must
멘티가 특정 세션에 출석(check-in)했다면, 이후 재방문·시간창 종료와 무관하게 해당 세션이 '출석완료'로 표시된다.
- Given 멘티가 세션에 출석 체크함, When '내 러닝'의 세션 목록을 다시 조회, Then 그 세션은 '출석완료'로 표시된다(출석 창이 닫혔더라도).
- 백엔드는 세션별 본인 출석 여부를 조회할 수 있는 read를 제공하고, FE는 이를 세션 목록에 반영한다.

### FR-6 세션 미완료여도 모임 완료 처리 가능 — Must
관리자의 모임 ③ 완료(T6)는 예정 세션이 모두 종료되지 않았더라도 수행할 수 있다.
- Given IN_PROGRESS 모임, 일부/전체 세션이 아직 미종료, When 관리자가 완료 처리, Then COMPLETED로 전이한다(세션 종료 게이트로 차단하지 않음).
- 불법 상태(비 IN_PROGRESS)에서의 완료는 기존대로 409(MEETING_INVALID_TRANSITION).

### FR-7 완료 후 멘토 수료 판정 버튼 — Must
소유 멘토가 자기 모임의 수료 판정(compute)을 버튼으로 직접 실행할 수 있다.
- Given 소유 멘토, 모임이 완료(또는 IN_PROGRESS), Then 운영 허브의 수료 판정 영역에 '수료 판정 실행' 버튼이 노출된다.
- When 클릭, Then `POST /api/meetings/{id}/completions/compute`를 호출하고 결과(멘티별 수료 후보/미수료)를 표시한다.
- 백엔드는 이미 owning-mentor OR admin을 허용하므로 권한 변경 없이 FE 진입점만 추가한다.

### FR-8 쪽지 확인 시 안읽음 뱃지 제거 — Must
쪽지 스레드를 열람(읽음 처리)하면 하단 내비의 안읽음 뱃지 수가 즉시 반영되어 줄어들거나 사라진다.
- Given 안읽음 쪽지가 있어 뱃지가 표시됨, When 해당 스레드를 열람하여 읽음 처리, Then 안읽음 카운트가 재조회되어 뱃지가 갱신(0이면 사라짐)된다.

### FR-9 관리자 모임 화면의 '개설 승인' 버튼 제거 — Must
관리자로 로그인한 모임 목록 화면(`/meetings`)에서 '개설 승인' 진입 버튼을 제거한다. 모든 관리 액션은 '관리' 탭(`/admin/meetings`)에서 수행한다.
- Given ADMIN 로그인, When 모임 목록을 본다, Then '개설 승인' 버튼이 노출되지 않는다('관리' 탭 진입점은 하단 내비로 충분).

### FR-10 관리 승인 화면 단계별 카운트 제거 — Must
관리 승인 화면(`/admin/meetings`)의 각 승인 단계 섹션 헤더에서 건수 표시(`(n)`)를 제거한다.
- Given 관리자가 승인 화면을 본다, Then 각 단계 섹션 제목에 개수 숫자가 표시되지 않는다.

### FR-11 다양한 케이스의 Flyway 초기 시드 데이터 — Must
데모/테스트를 위한 풍성한 초기 데이터를 Flyway 마이그레이션(V12)으로 등록한다.
- 다양한 유저: 복수 멘토·다수 멘티(기존 V10 admin, V11 멘토2·멘티7 시드를 활용/확장).
- 다양한 수업(모임): 상태별로 PENDING_APPROVAL·RECRUITING·READY_TO_START·IN_PROGRESS·COMPLETED·REJECTED·CANCELLED를 고르게 포함.
- 다양한 참여 상태: 신청(APPLIED)·취소(CANCELLED), 정원 마감/여유, 세션·출석(부분/전체)·수료 후보/확정, 사전설문 응답·과정 피드백 등을 케이스별로 포함.
- 멱등·비파괴: 자연키(employee_no/모임 제목) 기반 SELECT 삽입 + `ON CONFLICT DO NOTHING`으로 재실행/기존 데이터와 안전하게 공존.

## 비기능 요구사항 (NFR)
- **회귀 방지**: 기존 백엔드(계약/서비스/웹) + 프론트(vitest, tsc) 스위트가 그대로 green. 변경 영역엔 회귀 테스트 추가(팀 posture: bugfix = 특정 회귀 테스트 + 기존 green 유지).
- **계약 정합성**: 신규/변경 엔드포인트·DTO 필드(FR-5 세션별 출석 여부 등)는 `contracts/openapi.yaml`·`OpenApiContractTest`에 반영.
- **권한**: 관리 액션은 ADMIN, 세션/판정은 소유 멘토 or ADMIN으로 서버 재검증(403 유지).
- **에러/i18n**: 전역 `{code,message,details}` 스키마·HTTP 상태 규약 유지, 사용자 메시지 한글.
- **시드 안전성**: V12 시드는 운영 배포 전 정리 대상(개발용). 멱등·ON CONFLICT로 재적용 안전.

## 제약 (Constraints)
- 스택 고정: React/Vite, Spring Boot/JPA, PostgreSQL, 전부 로컬(docker-compose). 외부 SaaS 금지.
- 파이프라인/operation 단계 미실행(project.md Scope Overrides). 구현은 build-and-test까지.
- 신규 타입 simple name 충돌 금지(project.md), API camelCase / JPA snake_case 유지.
- Flyway 불변 마이그레이션 원칙: 기존 V1~V11 수정 금지, 신규 시드는 V12로 추가.

## 가정 (Assumptions)
- FR-5 세션별 출석 여부 조회를 위해 세션 목록 응답 또는 별도 read에 본인 출석 플래그를 결합한다(스키마 변경 없이 attendance read 조합).
- FR-3 이전 버튼은 브라우저 히스토리 기반(`navigate(-1)`)으로 충분(탭 루트 판별은 경로 매칭).
- FR-11 시드는 현재 실행 중 DB(V1~V9 적용, 수동 데이터 존재)에도 부팅 시 V10~V12가 순차 적용되어 공존.

## 범위 외 (Out of scope)
- U9 관리자 종합 모니터링/통계 대시보드(별도 Bolt 8 브랜치).
- 신규 도메인/기능 추가, CI/CD·배포·운영 단계.
- 쪽지 실시간 푸시(폴링 기반 유지).

## 미해결 질문 (Open questions)
- 없음(사용자가 11개 항목을 명시). 구현 세부는 code-generation에서 확정.

## 추적 매트릭스
| Req ID | 설명 | 우선순위 | 상태 | 영향 영역 |
|--------|------|---------|------|-----------|
| FR-1 | 활성 탭 재클릭 새로고침 | Must | Draft | FE AppShell/라우팅 |
| FR-2 | 자료실·피드백 진입 시 내 러닝 탭 유지 | Must | Draft | FE 라우팅(/my-learning 하위), MyLearningPage |
| FR-3 | 뎁스 이동 이전 버튼(우하단) | Must | Draft | FE 공용 BackButton + 하위 화면 |
| FR-4 | 사전설문 응답에 문항 표시 | Must | Draft | FE 설문 응답 열람, 문항 API 결합 |
| FR-5 | 출석완료 상태 유지 | Must | Draft | BE 세션 출석 read, FE 세션 목록 |
| FR-6 | 세션 미완료여도 완료 처리 | Must | Draft | BE MeetingApprovalService/게이트 |
| FR-7 | 완료 후 멘토 수료 판정 버튼 | Must | Draft | FE 운영 허브(BE 현행 유지) |
| FR-8 | 쪽지 확인 시 뱃지 제거 | Must | Draft | FE useUnreadCount/ThreadView |
| FR-9 | 모임 화면 개설승인 버튼 제거 | Must | Draft | FE MeetingListPage |
| FR-10 | 승인 화면 단계 카운트 제거 | Must | Draft | FE AdminApprovalPage |
| FR-11 | 다양한 케이스 Flyway 시드 | Must | Draft | BE db/migration V12 |
