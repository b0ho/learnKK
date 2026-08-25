<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
- 2026-08-25T05:20:00Z — code-generation = 디자인 시스템 구현. 확정 방향: 그린 브랜드 / 라이트만(다크 슬롯) / Pretendard(self-host) / 모던 미니멀 / 전 화면 일괄. shadcn/ui 유지, 토큰·타이포·컴포넌트 보강만. 기능·계약 무변경.
- 2026-08-25T05:20:00Z — 회귀 0 원칙: 기존 141 프론트 테스트가 로딩/빈/에러 상태의 data-testid, role="alert", 텍스트("불러오는 중..." 등)에 의존하므로, 상태 컴포넌트화 시 이 계약을 보존해야 함.

## Deviations
- 2026-08-25T05:20:00Z — 상태 UI(로딩/빈/에러)는 컴포넌트화하지 않고 토큰 리스타일만 적용 — 기존 141/135 테스트의 data-testid·role·한글 문자열 계약 보존(회귀 0 우선). 새 primitive(tooltip/avatar/dropdown-menu)는 추가만 하고 페이지 미배선(테스트 계약 보호).
- 2026-08-25T05:20:00Z — build 그린화를 위해 이번 범위와 무관한 사전 존재 타입 에러 3건(MeetingQuestionsEditPage·AdminApprovalPage.test·MyLearningPage.test) 최소 수정. 프론트 전역을 손대는 작업이라 함께 정리.

## Tradeoffs
- 2026-08-25T05:20:00Z — 폰트를 static 9종에서 **variable 단일 woff2**로 전환(리뷰어 제안) — 번들 ~13MB→~2MB, 모바일 우선(NFR1) 부합. 빌드·테스트 재검증 green.

## Open questions
- 2026-08-25T05:20:00Z — 다크 테마 값은 `.dark {}` 스캐폴드만 존재(토글 미배선) — 추후 대비 튜닝 후 활성화. 시각 검증: 로그인/가입 화면 모바일 폭에서 그린 브랜드·Pretendard·카드/버튼/라디오 폴리시 실제 렌더 확인(스크린샷).
