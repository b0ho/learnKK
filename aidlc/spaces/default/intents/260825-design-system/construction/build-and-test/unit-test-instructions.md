# Unit Test Instructions — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드). 출처: code-generation code-summary.md, intent-statement.md(회귀 0), 기존 vitest 규약. 프론트 전용 변경. -->

## 개요

시각 변경의 단위/컴포넌트 테스트 전략. 핵심 목표는 **회귀 0** — 기존 컴포넌트 테스트가 의존하는 data-testid·role·한글 문구 계약이 토큰 리스타일 후에도 유지됨을 보증한다.

## 실행

- `cd frontend && npx vitest run` (또는 `npm test`).
- 결과: **135 passed / 135** (28 files). 신규 테스트 추가 없음(시각 변경은 마크업/동작 미변경).

## 커버 범위

- 기존 컴포넌트 테스트(로그인·모임 목록/생성·내 러닝·쪽지·설문/피드백·admin 승인·AppShell 등)가 그대로 통과 → 상태 UI(로딩/빈/에러)의 testid·role·문구 계약 보존 확인.
- api 계층 단위 테스트(auth/users/meetings/admin/survey/feedback/errors) 무영향 통과.

## 신규/변경 테스트

- 없음. 토큰 값·CVA 클래스·폰트만 변경, 마크업/props/export 미변경이라 기존 스냅샷/쿼리 계약 유지.
- 사전 존재 테스트 2건은 타입 정합만 최소 수정(AdminApprovalPage.test mock 시그니처, MyLearningPage.test fixture `completed` 필드) — 단정 로직 불변.

## Assumptions & Open Questions

- **[decided]** 회귀 0 = 기존 135 테스트 통과. 시각 변경이라 신규 단위 테스트 불요.
- **[assumption]** 추가 primitive(tooltip/avatar/dropdown-menu)는 페이지 미배선이라 테스트 대상 아님(도입만).
