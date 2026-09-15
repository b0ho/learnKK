# Requirements — apply-button-state bugfix

## Intent 분석

"모집중 모임" 목록(`MeetingListPage`)에서 멘티가 자신의 신청 가능 여부를 **최초 로드 시점에 정확히** 인지하도록 한다. 현재는 (1) 이미 신청한 모임도 "신청" 버튼이 활성이라 클릭해야 중복임을 알게 되고, (2) 정원이 찬 모임도 "신청" 활성이라 클릭 후 409로만 마감을 알게 된다. 목표는 클릭 이전에 상태(신청완료/마감)를 시각적으로 드러내 헛클릭과 혼란을 없애는 것.

프로젝트 유형: Brownfield. 대상 표면은 좁고 잘 이해되어 있어 **Minimal depth**.

## Functional Requirements

### FR-1 내 신청 상태 반영 (Bug 1)
- 목록 로드 시, 로그인한 MENTEE인 경우 `enrollmentsApi.listMine()`로 본인의 신청 내역을 조회한다.
- 응답 중 `status === 'APPLIED'`인 `meetingId`에 해당하는 카드는 신청 버튼을 **"신청완료" 라벨 + 비활성**으로 렌더한다(신청 직후 상태와 동일).
- 비로그인 사용자 및 MENTOR에게는 기존 동작을 유지한다(모집 목록 자체는 비로그인 공개, 신청 버튼은 MENTEE에게만 노출).
- 신청 내역 조회 실패는 목록 렌더를 막지 않는다(신청 상태 반영만 생략, 목록은 정상 표시).

### FR-2 정원 마감 표기 (Bug 2)
- 백엔드 `MeetingSummary`에 다음을 추가한다:
  - `enrolledCount`: 해당 모임의 `status=APPLIED` 신청 인원 수
  - `full`: 파생 마감 여부 (`enrolledCount >= capacity`)
- `listRecruiting`(및 동일 DTO를 쓰는 목록 경로)에서 모임별 APPLIED 인원을 집계해 위 필드를 채운다.
- FE는 `full === true`이고 **내가 신청하지 않은** 모임에 대해 상태 배지 영역에 **"마감" 배지**를 노출하고, 신청 버튼을 **"마감" 라벨 + 비활성**으로 렌더한다.

### FR-3 상태 우선순위
- 한 카드에서 "내가 신청함"과 "정원 마감"이 동시에 성립하면 **"신청완료"(내 신청 상태)를 우선** 표시한다.
- 우선순위: 신청완료 > 마감 > (기본) 신청 가능.

### FR-4 계약/타입 정합
- OpenAPI 계약(`contracts/openapi.yaml`)의 `MeetingSummary` 스키마에 `enrolledCount`, `full`을 반영한다.
- 프론트 `MeetingSummary` 타입(`frontend/src/api/types.ts`)에 동일 필드를 반영한다.
- 기존 목 데이터/테스트 호환을 위해 신규 필드는 프론트에서 optional로 두되, 없을 때는 마감 미표시로 안전하게 폴백한다.

## Non-Functional Requirements
- **판정 일관성**: 표시용 마감 판정 인원 기준은 백엔드 정원 체크(`countByMeetingIdAndStatus(APPLIED)`)와 동일해야 한다.
- **성능**: 목록 인원 집계는 모임 수 N에 대해 N+1 쿼리를 피한다(그룹 카운트 1회 조회 등 배치 집계).
- **회귀 방지**: 기존 신청 성공/409 매핑/멘토·비로그인 동작은 변경 없이 유지.
- **접근성**: 비활성 버튼은 disabled 속성으로 처리하고, 마감/신청완료는 텍스트로도 식별 가능해야 한다(색상 단독 의존 금지).

## Constraints
- Tech stack 고정: FE React, BE Java Spring, DB PostgreSQL, 전부 로컬.
- JSON 필드 camelCase, JPA 물리 네이밍 snake_case.
- JPA 엔티티를 컨트롤러 응답으로 노출 금지 — DTO만 사용.
- 사용자 노출 문구는 한글, 기술 용어는 영어 유지.
- 이 프로젝트 스코프상 구현은 build-and-test(3.6)에서 종료하며 ci-pipeline/operation은 실행하지 않는다.

## Assumptions
- `enrollmentsApi.listMine()`는 인증된 호출자의 전체 신청 내역을 반환한다(현행 `/api/enrollments/mine`).
- `RECRUITING` 상태 모임만 목록에 노출되므로 마감 표기는 RECRUITING 카드 범위에서만 의미를 갖는다.
- 목록은 페이지네이션(page/size)되며, 인원 집계는 현재 페이지의 모임 집합에 대해 수행하면 충분하다.

## Out of Scope
- 정원 마감 모임의 목록 필터링/정렬 변경(마감을 숨기거나 뒤로 보내는 등) — 표기만 하고 노출 정책은 그대로.
- 실시간 인원 갱신(웹소켓 등) — 로드 시점 스냅샷 기준.
- 신청 취소 후 즉시 재조회 등 목록 이외 화면 동작.
- ci-pipeline 및 operation phase 전반.

## Open Questions
- 없음(핵심 결정 Q1~Q4 확정). 인원 집계 쿼리 방식(그룹 카운트 vs 파생 컬럼)은 code-generation 단계에서 성능·단순성 기준으로 확정한다.
