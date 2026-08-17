# User Stories — Observation Diary

<!-- Auto-created at stage start. Maintained by the orchestrator. Not hand-edited by the user. -->

## Interpretations

- 2026-08-17T11:11:40Z — requirements.md가 9개 FR 그룹으로 이미 상세. 3역할(멘토/멘티/관리자)이 확정 페르소나라 페르소나 작업은 확정분 정리 위주. 인수기준은 inception 규칙에 따라 Given/When/Then(BDD)로 작성.

## Deviations

## Tradeoffs

- 2026-08-17T11:40:00Z — mob 3인 기고가 모두 보완적(모순 없음)이라 human judgment call/round 2 없이 리드가 전량 통합. 신규 스토리 4개(인증·멘티현황·멘토허브·멘티수료뷰)는 신규 스코프가 아니라 기존 FR + 푸시없음(FR5.2)이 함축한 여정 공백 메움으로 판단해 추가.
- 2026-08-17T11:41:00Z — 모임 상태머신을 스토리 전반 분산에서 단일 canonical 계약(#3)으로 승격. 독립 병렬 interface 불일치가 지배 리스크라 도메인 타입 계약으로 못박음(developer 기고 반영).

## Open questions

- 2026-08-17T11:42:00Z — reviewer S4/S6(시작대기 취소가 FR3.5 문언 넘어섬), S7(US-7.2 예시 문구), 역할 겸직은 functional-design에서 사용자 확정/정련으로 이월.
- 2026-08-17T12:30:00Z — 게이트 cycle2 사용자 수정: 사번 가입+사번 기반 중복방지(히든 IP 대체), 사전설문 응답을 ②시작 후로 이동(신청 분리+US-3.6 신설), 주차별 세션 일정(멘토 지정·변경·복수)+멘티 팝업 출석+분모=전체 예정 세션. requirements.md도 [rev-us]로 정합화(상류 완료분 역수정) — project.md Decided 2건(히든 IP·신청 시 설문) supersede는 flag만 하고 하류 reconcile로 이월. reviewer iter1 NOT-READY(카운트 오기 B1)→정정→iter2 READY.
