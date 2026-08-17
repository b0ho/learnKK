# Refined Mockups — Observation Diary

<!-- Auto-created at stage start. Maintained by the orchestrator. Not hand-edited by the user. -->

## Interpretations

- 2026-08-17T13:00:00Z — rough-mockups 와이어프레임(9화면)은 rev2 이전 모델(멘토 출석창 제어, 신청 시 설문 응답)을 반영. refined mockups는 현재 requirements/stories(rev2: 사번, 사전설문 ②후, 멘토 세션 일정+멘티 팝업 출석, 출석 분모=세션)를 기준으로 갱신한다 — 상류 와이어프레임 대비 변경점을 명시.

## Deviations

## Tradeoffs

- 2026-08-17T13:20:00Z — Q3(복수선택)에 사용자가 A만 표기했으나 B/C/D는 승인된 stories의 현실이라 A만 반영 시 mockups가 stories와 모순. 추측 대신 FQ1 후속질문으로 확인 → A+B+C+D 전부 반영 확정. (grounded-artifact: 승인분과 모순되는 under-answer는 되물어 정합화)
- 2026-08-17T13:25:00Z — mockups는 신규 파일이라 최소-diff 이슈는 없으나, 리뷰어 수정 반영 시 diff 위생 규칙대로 str_replace 타깃 편집만 사용(화면6 US-3.4 추가, 화면5 unicode 트리→ASCII, 매핑표). 전체 재작성 안 함.

## Open questions

- 2026-08-17T13:26:00Z — 세션 변경 통지 UI·출석 유효 시간창·사전설문 미응답자 리마인드·반려 사유 UI·shadcn 최종 채택은 functional-design/구현 이월.
- 2026-08-17T14:00:00Z — 학습 의례 도중 사용자가 도메인 변경 제시(멘토 완료 인정 신청 제거, 모임 종료 시 관리자 직접 ③). 해석 확인(A) 후 mockups·interaction-spec·stories·personas·requirements 5파일 최소-diff 정합화. 리뷰어 iter1 잔여 2건(US-2.3 완료인정 clause, mentorComplete 키) NOT-READY → 정정 → iter2 READY. 리뷰어가 잔여 참조까지 잡아낸 게 유효했음(전면 grep + 상류 정합 검증).
