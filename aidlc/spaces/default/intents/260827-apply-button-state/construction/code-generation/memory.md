<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
- 2026-08-27T01:31:16Z — bugfix 스코프라 unit-of-work/functional-design/nfr-design 산출물 없음 → requirements.md + brownfield 소스 직접 읽기로 구현 범위 확정(스테이지 프로토콜 허용).
- 2026-08-27T01:31:16Z — 단일 unit 이름을 인텐트 슬러그 `apply-button-state`로 사용(units-generation 미실행).

## Deviations
- 2026-08-27T01:31:16Z — MeetingSummary 인원 집계를 MeetingService 내부가 아니라 컨트롤러 계층(MeetingController)에서 EnrollmentService와 조합해 채움. 이유: EnrollmentService→MeetingService 의존이 이미 존재해 MeetingService→EnrollmentService 주입은 Spring 빈 순환을 만든다. 또 ADR-007/project.md(cid:code-generation:c5)는 "meeting 서비스가 enrollment 테이블을 직접 읽지 않는다"를 규정 → 컨트롤러 조합이 순환 없이 경계를 지키는 최소 변경.

## Tradeoffs
- 2026-08-27T01:31:16Z — 대안: (a) hexagonal read-port(인터페이스+어댑터) — 경계상 가장 깔끔하나 bugfix에 클래스 2개 추가 과다. (b) MeetingService에 EnrollmentRepository 직접 주입 — 경계 규칙 위반. (c) 컨트롤러 조합 — 채택(최소·순환없음·경계준수). code-summary에 기록.
- 2026-08-27T01:31:16Z — enrolledCount/full은 모집 목록(listRecruiting) 경로에서만 정확히 채움. 관리자 큐(listByStatus)·멘토 mine은 이 bugfix 범위 밖(신청/마감 UI 미표시)이라 from(Meeting) 기본값(enrolledCount=0, full=false) 유지. code-summary에 스코프 한정 명시.

## Open questions
- 2026-08-27T01:31:16Z — [계획 게이트에서 사람에게 확인] 현재 main의 백엔드 테스트 컴파일이 이미 깨져 있음(11 errors: MeetingResponse 13번째 인자 mentorCompletionStatus 누락 9곳 + MeetingSummary 6-arg 2곳, 이전 ux-bugfixes-2에서 record 필드 추가 후 테스트 미갱신). bugfix 규칙은 "기존 suite green 유지"인데 이미 red → 내 회귀 테스트 실행을 위해 이 사전 존재 컴파일 에러도 함께 수리해야 함(기계적). 범위 확장을 사람에게 확인 필요.
