<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

- 2026-07-31T04:00:00Z — (reviewer READY, 비차단) refined-mockups 이월 항목: (1) 과정 설문·멘토 피드백 확인 전용 화면 추가, (2) 관리자 승인/반려 상세 검토 화면·반려 사유 UI, (3) 상태 용어 정합('마감' vs 라이프사이클) 통일, (4) 화면1 검색창 유지/제거 확정.
- 2026-07-31T04:00:30Z — IA 역추천: intent 3탭 골격 유지 + 쪽지를 전역 헤더 아이콘으로(4번째 탭 대신). 탭2는 역할 적응형(멘티/멘토/관리자).

- 2026-07-31T04:30:00Z — (도메인 정정, 사용자) 관리자 승인 4지점(①개설 ②시작 ③멘토완료 ④멘티수료). 멘티 수료=시스템 자동판정+관리자 승인(멘토 소관 아님). 멘토=모임 완료 인정 신청(관리자 승인③)+피드백 확인. 멘티 피드백은 관리자도 열람. wireframes 화면5·6, user-flow Flow B·C·상태전이 반영. project.md ## Decided에 3건 persist. 아티팩트 변경으로 reviewer iteration 2 재실행.

- 2026-07-31T04:40:00Z — reviewer iteration 2 READY. 비차단 이월(refined-mockups): 과정설문/피드백 열람 전용 화면, 관리자 승인/반려 검토 화면(4종), 상태 용어 통일([마감]/[개설신청]), 화면5의 stray 빈 코드블록 정리, 화면1 검색창 유지/제거 확정.

- 2026-07-31T04:50:00Z — (도메인 정정, 사용자) 사전 설문 문항 틀은 멘토가 모임 개설 시 정의, 멘티는 신청 시 응답. user-flow Flow A·B, wireframes 화면3·7 수정. project.md ## Decided persist. 게이트 rejected 후 수정 — 최종 리뷰(iteration 3) 예정.