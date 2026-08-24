# 요구사항 — learnKK UX/기능 버그픽스 (ux-bugfixes)

## Intent 분석
learnKK(브라운필드; `business-overview.md`·`architecture.md`·`code-structure.md` 참조)에서 멘토/멘티/관리자 흐름을 실제 사용하며 발견된 12건의 UX·기능 결함을 수정한다. 목표는 관리자 승인 운영성 개선, 멘토 세션 관리 완성도, 설문/자료 UX 개선, 신청 재시도 허용이다. 신규 도메인 추가가 아니라 기존 U3~U8 동작의 보정이다.

## 기능 요구사항 (FR)

### FR-1 설문 선택지 쉼표 입력 (#1) — Must
`SurveyBuilder`의 CHOICE 선택지 입력창에서 쉼표를 입력할 수 있어야 한다.
- Given CHOICE 유형 문항 편집, When "초급, 중급" 처럼 쉼표를 입력, Then 입력한 쉼표/텍스트가 사라지지 않고 그대로 유지된다.
- 저장 시 선택지는 쉼표 기준으로 trim·빈값 제거되어 배열로 저장된다.

### FR-2 모임 승인 화면을 대기 목록으로 (#2) — Must
관리자 승인 화면은 모임 ID를 조회하는 방식이 아니라, 처리 대기 모임을 **목록**으로 보여주고 그 안에서 승인한다.
- Given 관리자로 `/admin/meetings` 진입, Then 처리 대기 모임들이 목록으로 표시된다(ID 입력 없이).
- 백엔드는 상태별 모임 목록을 관리자에게 제공한다(신규 조회 엔드포인트).

### FR-3 승인 유형별 영역 구분 (#3) — Must
목록은 승인 유형(① 개설 승인 대기 / 모집확정 대기 / ② 시작 대기 / ③ 완료 대기·수료 판정)별로 영역(섹션)을 나눠 표시한다.
- Given 여러 상태의 모임 존재, Then 각 모임이 자신의 상태 영역에 배치되고 해당 영역의 액션만 노출된다.

### FR-4 관리자 네비게이션 '관리' (#4) — Must
관리자는 '내 러닝'이 없으므로, 하단 네비의 해당 자리에 '관리'(→ `/admin/meetings`) 진입점을 노출한다.
- Given ADMIN 로그인, Then 네비에 '관리' 탭이 보이고 '내 러닝'은 보이지 않는다.
- 멘토/멘티는 기존대로 '내 러닝'을 본다.

### FR-5 승인 되돌리기 (#5) — Must
관리자는 전진 승인을 직전 단계로 되돌릴 수 있다(Q1=A).
- RECRUITING→PENDING_APPROVAL, READY_TO_START→RECRUITING, IN_PROGRESS→READY_TO_START, COMPLETED→IN_PROGRESS.
- 반려(REJECTED)·모집취소(CANCELLED)는 되돌리기 대상이 아니다.
- Given 잘못 승인된 모임, When 관리자가 '되돌리기', Then 직전 상태로 원자적 역전이한다. 되돌릴 수 없는 상태면 409(MEETING_INVALID_TRANSITION).

### FR-6 승인 확인 다이얼로그 (#6) — Must
모든 관리자 승인/되돌리기 액션은 실행 전 확인 메시지를 거친다.
- Given 승인/시작/완료/모집확정/되돌리기 버튼 클릭, Then 확인 다이얼로그가 뜨고, 확인 후에만 실행된다. (반려·모집취소는 기존 사유 입력 다이얼로그가 확인 역할)

### FR-7 세션 삭제 (#7) — Must
멘토는 자기 모임의 세션을 등록 후 삭제할 수 있다.
- Given 소유 멘토, When 세션 삭제, Then 세션과 그 출석 기록이 함께 삭제(CASCADE)되고 출석율이 재계산된다(Q3=A).
- 다른 멘토/멘티는 403(SESSION_FORBIDDEN).

### FR-8 세션 완료 처리 (#8) — Must
멘토는 세션을 완료 처리할 수 있고, 완료된 세션은 시간과 무관하게 '종료'로 간주된다(Q2=B).
- Given 소유 멘토, When 세션 완료 처리, Then 해당 세션은 종료 상태가 되어 모임 완료(③ T6) 게이트 판정에서 종료로 취급된다.
- 세션 종료 판정 = 수동 완료 OR 시간창(scheduledAt+window) 경과(자동, 현행 유지).
- UI에 완료/진행 상태가 표시된다.

### FR-9 자료실·공지 보기 강조 (#9) — Must
멘티/멘토의 '내 러닝'에서 자료실·공지 진입을 텍스트 링크가 아닌 버튼으로 시각적으로 강조한다.
- Given 멘티가 '내 러닝'의 참여(APPLIED) 모임 카드를 본다, Then '자료실·공지 보기' 진입점이 `<Button>` 요소(variant outline, 아이콘 동반)로 렌더된다(기존 text link 아님).
- Given 멘토가 운영 허브의 모임 카드를 본다, Then '자료실·공지 관리' 진입점이 동일하게 `<Button>` 요소로 렌더된다.
- 회귀 기준: 해당 진입점의 `data-testid`가 유지되고 role="link"가 아닌 버튼형으로 노출된다.

### FR-10 사전설문 문항 편집 진입점 (#10) — Must
멘토가 개설 이후에도 사전설문 문항을 추가/수정할 수 있는 진입점을 제공한다.
- 편집 허용 상태: PENDING_APPROVAL·RECRUITING·READY_TO_START(모집확정까지). IN_PROGRESS부터 잠금(현행 백엔드 유지, Q4=A).
- Given 소유 멘토·편집 허용 상태, Then 운영 허브에서 문항 편집 화면에 진입해 저장할 수 있다.

### FR-11 피드백/사전설문 분리 표시 (#11) — Must
멘토/관리자 열람 화면에서 과정 피드백과 사전설문 응답을 별도 섹션으로 나눠 보여준다(현재는 멘티별 카드에 혼재).
- Given 멘토/관리자가 열람 화면(`/meetings/{id}/feedback-view`) 진입, Then 과정 피드백과 사전설문 응답이 각각 별도 헤더 섹션으로 렌더되고 한 카드 안에 혼재되지 않는다.
- 각 섹션은 비어 있을 때 고유의 empty 상태 메시지를 표시한다.

### FR-12 취소 후 재신청 (#12) — Must
멘티가 신청을 취소한 뒤 같은 모임에 다시 신청할 수 있다(Q5=A).
- Given 취소(CANCELLED)한 신청, 모임이 RECRUITING이고 정원 여유, When 재신청, Then 기존 레코드를 APPLIED로 재활성화(신청일 갱신)한다.
- 이미 APPLIED면 409(ENROLLMENT_DUPLICATE), 정원 마감이면 409(ENROLLMENT_FULL).

## 비기능 요구사항 (NFR)
- **회귀 방지**: 기존 테스트 스위트(백엔드 계약/서비스/웹, 프론트 tsc)가 그대로 green. 변경 도메인엔 회귀 테스트 추가(팀 posture: bugfix = 특정 버그 회귀 테스트 + 기존 green 유지).
- **계약 정합성**: 신규/변경 엔드포인트·DTO 필드는 `contracts/openapi.yaml`과 `OpenApiContractTest`에 반영되어 계약 테스트 green.
- **권한**: 관리자 전용 액션은 ADMIN, 세션/문항 관리는 소유 멘토로 서버에서 재검증(403).
- **원자성**: 상태 역전이는 기존 조건부 UPDATE(transitionStatus) 패턴으로 원자적.
- **에러**: 전역 `{code,message,details}` 스키마·HTTP 상태 규약 유지, 사용자 메시지 한글.

## 제약 (Constraints)
- 스택 고정: React/Vite, Spring Boot/JPA, PostgreSQL, 전부 로컬(docker-compose). 외부 SaaS 금지.
- 파이프라인/operation 단계 미실행(project.md Scope Overrides). 구현은 build-and-test까지.
- 신규 도메인 타입 simple name은 기존과 충돌 금지(project.md), API camelCase / JPA snake_case 유지.

## 가정 (Assumptions)
- 세션 완료 상태 저장을 위해 `meeting_session`에 완료 플래그 컬럼 추가(Flyway V9). attendance FK는 이미 ON DELETE CASCADE.
- 관리자 상태별 목록은 페이지네이션 없이 상태별 조회로 충분(운영 규모 소규모 가정).
- 사전설문 응답은 IN_PROGRESS부터 시작하므로 문항 편집(≤READY_TO_START)과 시간상 겹치지 않아 문항-응답 불일치 없음.

## 범위 외 (Out of scope)
- U9 관리자 종합 모니터링/통계 대시보드(Bolt 8).
- 반려/모집취소 되돌리기(Q1=A로 제외).
- 신청 이력 다건 보관(Q5=A로 재활성화 방식 채택, UNIQUE 완화 안 함).
- CI/CD·배포·운영.

## 미해결 질문 (Open questions)
- 없음(핵심 설계 결정 5건 확정). 구현 세부는 code-generation에서 결정.

## 추적 매트릭스
| Req ID | 설명 | 우선순위 | 상태 | 영향 영역 |
|--------|------|---------|------|-----------|
| FR-1 | 설문 쉼표 입력 | Must | Approved | FE SurveyBuilder |
| FR-2 | 승인 대기 목록 | Must | Approved | FE AdminApprovalPage, BE 목록 API |
| FR-3 | 승인 유형 영역화 | Must | Approved | FE AdminApprovalPage |
| FR-4 | 관리자 '관리' 네비 | Must | Approved | FE AppShell |
| FR-5 | 승인 되돌리기 | Must | Approved | BE MeetingApprovalService, FE |
| FR-6 | 승인 확인 다이얼로그 | Must | Approved | FE AdminApprovalPage |
| FR-7 | 세션 삭제 | Must | Approved | BE SessionService, FE MentorSessions |
| FR-8 | 세션 완료 처리 | Must | Approved | BE 세션(V9), FE |
| FR-9 | 자료실 강조 | Must | Approved | FE MyLearningPage |
| FR-10 | 문항 편집 진입점 | Must | Approved | FE (BE 현행 유지) |
| FR-11 | 피드백/설문 분리 | Must | Approved | FE FeedbackViewPage |
| FR-12 | 취소 후 재신청 | Must | Approved | BE EnrollmentService, FE |
