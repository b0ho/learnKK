# Interaction Specification — learnKK (런크크)

<!-- refined-mockups 산출물. 컴포넌트 명세는 component-spec-template 형식을 따른다. 모바일 웹뷰 기준. 출처: mockups.md, user-stories(rev2), requirements(NFR), team-practices. 반응형은 모바일 세로 단일 기준이라 tablet/desktop 행은 "범위 밖(확대 대응)"으로 표기. -->

## 상호작용 패턴 개요

- **네비게이션:** 하단 3탭(모임/내 러닝/내정보) + 전역 헤더 쪽지 아이콘. 탭2(내 러닝)는 역할 적응형(멘티/멘토/관리자).
- **모달/다이얼로그:** 출석 팝업(화면10), 세션 일정 추가/변경(화면5), 승인 큐 반려 사유 입력. 모두 role=dialog·포커스 트랩·ESC 닫기.
- **인라인 상태:** 목록/상세의 로딩(스켈레톤)·빈·오류(재시도) 상태는 인라인 표시(CC-3).
- **알림:** 푸시 없음 — 인앱 뱃지(쪽지·공지·설문 미응답) + 접속 시 폴링/새로고침 반영(US-5.1).
- **폼:** 가입(사번 포함)·개설·사전설문 응답은 필드 검증 + aria-live 오류.

아래는 rev2·상호작용 핵심 컴포넌트의 상세 명세다(전 컴포넌트 열거 아님 — functional-design에서 확장).

---

## AttendanceCheckDialog (출석 팝업) — US-6.3 [rev2]

| Field | Value |
|---|---|
| Component | AttendanceCheckDialog |
| Description | 예정 세션 시간에 참여 멘티에게 제공되는 self check-in 모달 |
| Category | feedback |

### States

| State | Description | Trigger |
|---|---|---|
| default(open) | 유효 시간창 내 출석 가능 | 예정 세션 시각 도래 + 멘티 접속 |
| checked | 이미 출석 완료(멱등) | 해당 세션 출석 기록 존재 |
| window-closed | 유효 시간창 밖 | 세션 시각 허용 범위 경과 |
| loading | 출석 제출 중 | 출석 버튼 클릭 |
| error | 제출 실패 | 네트워크/서버 오류 |

### Props / Inputs

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| sessionId | string | yes | — | 대상 세션 |
| open | boolean | yes | false | 표시 여부 |
| status | string | yes | default | default/checked/window-closed/loading/error |
| onCheck | function | yes | — | 출석 체크 제출 핸들러 |
| onDismiss | function | yes | — | "나중에"/닫기 |

### Responsive Behaviour

| Breakpoint | Behaviour |
|---|---|
| mobile (기준) | 화면 중앙 모달, 폭 90% 상한 |
| tablet/desktop | 범위 밖(중앙 모달 확대 대응만) |

### Accessibility

| Requirement | Implementation |
|---|---|
| ARIA role | dialog, aria-modal=true |
| Keyboard interaction | Tab 순환(포커스 트랩), Enter=출석, ESC=닫기 |
| Label | aria-labelledby=제목("출석 체크"), 세션 정보 aria-describedby |
| Contrast ratio | WCAG AA(텍스트 4.5:1, UI 3:1) |
| Screen reader | 열림 시 제목+세션 안내, 상태(완료/시간지남) aria-live 안내 |
| Focus management | 열림 시 출석 버튼에 포커스, 닫힘 시 트리거로 복귀 |

---

## SessionScheduleEditor (세션 일정 관리) — US-6.2 [rev2]

| Field | Value |
|---|---|
| Component | SessionScheduleEditor |
| Description | 멘토가 주차별 세션의 날짜·시간을 추가/변경(주차당 복수) |
| Category | input |

### States

| State | Description | Trigger |
|---|---|---|
| default | 세션 목록 표시 | 진행중 모임 열람 |
| empty | 세션 없음 | 세션 미등록 |
| editing | 날짜·시간 입력 다이얼로그 | [+세션]/[변경] 클릭 |
| saving | 저장 중 | 저장 클릭 |
| error | 저장 실패 | 검증/서버 오류 |

### Props / Inputs

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| meetingId | string | yes | — | 대상 모임 |
| sessions | object[] | yes | [] | {week, datetime} 목록 |
| onAdd | function | yes | — | 세션 추가 |
| onChange | function | yes | — | 세션 날짜·시간 변경 |

### Responsive Behaviour

| Breakpoint | Behaviour |
|---|---|
| mobile (기준) | 세로 리스트 + 하단 시트 형태 날짜/시간 입력 |
| tablet/desktop | 범위 밖 |

### Accessibility

| Requirement | Implementation |
|---|---|
| ARIA role | list(세션) + dialog(편집) |
| Keyboard interaction | Tab 이동, 날짜/시간 입력 키보드 접근, ESC 취소 |
| Label | 각 세션 "N주차 요일 HH:MM" 라벨, 입력 필드 label 연결 |
| Contrast ratio | WCAG AA |
| Screen reader | 변경 저장 시 "세션이 변경되었습니다" aria-live |
| Focus management | 편집 다이얼로그 포커스 트랩, 저장 후 목록 복귀 |

---

## SignupForm (가입 폼, 사번 포함) — US-1.1 [rev2]

| Field | Value |
|---|---|
| Component | SignupForm |
| Description | 닉네임·비밀번호·사번으로 가입(승인 없음) |
| Category | input |

### States

| State | Description | Trigger |
|---|---|---|
| default | 초기 입력 | 가입 화면 진입 |
| validating | 필드 검증 | blur/제출 |
| duplicate | 닉네임/사번 중복 | 서버 유일성 검증 실패 |
| submitting | 제출 중 | 가입 클릭 |
| error | 제출 실패 | 서버 오류 |

### Props / Inputs

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| onSubmit | function | yes | — | {nickname, password, employeeNo} 제출 |

### Responsive Behaviour

| Breakpoint | Behaviour |
|---|---|
| mobile (기준) | 단일 컬럼 폼 |
| tablet/desktop | 범위 밖(중앙 정렬 확대) |

### Accessibility

| Requirement | Implementation |
|---|---|
| ARIA role | form |
| Keyboard interaction | Tab 순서 사번→닉네임→비밀번호→제출 (사번 맨 위, rev4) |
| Label | 각 입력 label 연결(사번 포함), 필수 aria-required |
| Contrast ratio | WCAG AA |
| Screen reader | 중복/검증 오류 aria-live=assertive, 필드 aria-invalid |
| Focus management | 제출 오류 시 첫 오류 필드로 포커스 |

---

## MeetingCard (모임 카드) — US-3.1

| Field | Value |
|---|---|
| Component | MeetingCard |
| Description | 목록의 모임 요약 카드(상태 뱃지·정원·해시태그) |
| Category | display |

### States

| State | Description | Trigger |
|---|---|---|
| default | 카드 표시 | 목록 렌더 |
| focus | 키보드 포커스 | Tab |
| full | 정원 마감 | 정원 소진(시작대기/모집마감 라벨) |

### Props / Inputs

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| meeting | object | yes | — | {title, topic, weeks, capacity, enrolled, status, tags} |
| onOpen | function | yes | — | 상세 진입 |

### Responsive Behaviour

| Breakpoint | Behaviour |
|---|---|
| mobile (기준) | 풀폭 카드 세로 스택 |
| tablet/desktop | 범위 밖 |

### Accessibility

| Requirement | Implementation |
|---|---|
| ARIA role | link(카드 전체 클릭) |
| Keyboard interaction | Tab 포커스, Enter 진입 |
| Label | 카드 제목+상태 텍스트(뱃지 색상 단독 의존 금지) |
| Contrast ratio | WCAG AA |
| Screen reader | "제목, 상태, 정원 n/N, 모집마감일" 순 낭독 |
| Focus management | 포커스 링 표시 |

---

## ApprovalQueue (승인 큐, 반려 사유) — US-9.1

| Field | Value |
|---|---|
| Component | ApprovalQueue |
| Description | 관리자 4지점(①②③④) 승인/반려 처리 |
| Category | navigation/feedback |

### States

| State | Description | Trigger |
|---|---|---|
| default | 큐 목록(건수) | 모니터링 진입 |
| empty | 대기 없음 | 큐 비어있음 |
| rejecting | 반려 사유 입력 다이얼로그 | [반려] 클릭 |
| processing | 승인/반려 처리 중 | 확정 |
| error | 처리 실패 | 서버 오류 |

### Props / Inputs

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| queues | object | yes | — | {create, start, meetingComplete, menteeComplete} 건수·항목 (③=관리자 직접 모임 완료) |
| onApprove | function | yes | — | 승인 |
| onReject | function | yes | — | 반려(+사유) |

### Responsive Behaviour

| Breakpoint | Behaviour |
|---|---|
| mobile (기준) | 큐 4종 세로 리스트 + 항목 상세 시트 |
| tablet/desktop | 범위 밖 |

### Accessibility

| Requirement | Implementation |
|---|---|
| ARIA role | list + dialog(반려 사유) |
| Keyboard interaction | Tab 이동, 반려 사유 textarea 접근, ESC 취소 |
| Label | 각 큐 "① 개설 승인 (3건)" 건수 텍스트 병기 |
| Contrast ratio | WCAG AA |
| Screen reader | 처리 결과 aria-live |
| Focus management | 반려 다이얼로그 포커스 트랩 |

---

## 화면 전이(주요 흐름 요약)

- 목록(1) → 상세(2) → [신청하기] → 신청완료(선착순). (설문 없음)
- ②시작 후: 내러닝(4) "사전설문 응답하기" → 설문(3) 제출.
- ②시작 후: 멘토 관리(5) 세션 추가 → 멘티 내러닝(4)에 일정 반영 → 세션 시각 도래 → 출석 팝업(10) → 멘티 체크.
- 전 세션 종료 → 관리자가 ③(모임 완료) 직접 처리(6, rev3: 멘토 신청 없음); 시스템 자동 수료 판정 → 관리자 ④승인(6) → 멘티 수료 확정(4/7.4 표시).

## Assumptions & Open Questions

- 반응형 tablet/desktop는 이번 범위 밖(모바일 세로 단일). design-system-mapping의 확대 대응 참조.
- 출석 유효 시간창·세션 변경 통지·반려 사유 필수 여부는 functional-design 확정.
