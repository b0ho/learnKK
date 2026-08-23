# Code Generation — Observation Diary (Bolt 3 Enrollment)

<!-- 오케스트레이터 유지. 손으로 편집하지 않음. code-generation 스테이지 관측 로그. -->

## Interpretations

- 2026-08-23T11:40:00Z — Bolt 3(Enrollment, U4) 대상. Bolt 1·2 패턴 동일: memory_path {unit-name}을 bolt 레벨(`construction/bolt3-enrollment/`)로 해석. 설계는 설계 intent(`260731-learnkk-crew`)의 `construction/U4-enrollment/`에서 상속.
- 2026-08-23T11:40:00Z — Bolt 3 범위(bolt-plan): 선착순 신청·정원/중복 제어·취소·신청자 목록·멘티 현황. Brownfield — Bolt 1/2 코드에 신규 enrollment 모듈 추가 + 기존 시임 배선.

## Deviations

- (실행 중 기록)

## Tradeoffs

- 2026-08-23T11:40:00Z — [concurrency] BR-U4-1 정원 무결성: 설계가 제시한 (a) 어드바이저리 락 `pg_advisory_xact_lock(meetingId)` 채택(모임 단위 직렬화, U3 meeting 행 미잠금 — 모듈 소유 규칙 준수). count(APPLIED)<capacity 후 insert, `unique(meeting_id,mentee_id)`가 중복 백스톱. (b) SERIALIZABLE 대비 재시도 로직 불필요·구현 단순.
- 2026-08-23T11:40:00Z — [read-port] U4→U3 정원·상태 read는 `MeetingService.getMeeting`(MeetingResponse의 capacity/status/mentorId) 직접 호출로 해소(U4 depends_on U3, 순환 아님). U3 meeting 테이블 직접 접근 안 함.
- 2026-08-23T11:40:00Z — [seam wiring] Bolt 2가 남긴 U3 운영 허브 신청자 placeholder를 이번에 listApplicants로 배선(U3→U4, ADR-007 R-1, FE 화면 조합). 멘티 현황(MyLearningPage MENTEE placeholder)을 listMyEnrollments로 대체.
- 2026-08-23T11:40:00Z — [FE compose] 멘티 현황의 세션 일정(U5) 조합은 U5 미구현(Bolt 6) → placeholder 유지. 모임 정보는 getMeeting FE 조합.

## Open questions

- 2026-08-23T11:40:00Z — [env] 선착순 동시성 통합 테스트(잔여 1석 N-thread 경합)는 Testcontainers 필요 — 이 환경(Windows/Rancher JNA) 미실행 예상. 라이브 API E2E(병렬 curl)로 보완 검증 예정.
- 2026-08-23T11:40:00Z — [note] Principal에 isMentee() 부재 → 추가(isMentor/isAdmin 미러). ci-pipeline·operation은 project.md Scope Override로 build-and-test 후 SKIP 예정(Bolt 2와 동일, birth 시 미상속).

- 2026-08-23T12:30:00Z — [verification] 라이브 E2E(실행 앱 + 실제 Postgres, V4 적용) 16/16 통과. **핵심: capacity=1 + 3 병렬 apply → 정확히 1×201 + 2×409, DB APPLIED count=1 (overbooking 금지 실증).** 중복 409, 취소→빈자리 복귀→재신청 201, 비멘티 apply 403, 소유자외 listApplicants 403, IN_PROGRESS 후 취소 409, PENDING 신청 409. 어드바이저리 락 방식이 실 DB에서 무결성 보장 확인. (Testcontainers 동시성 테스트는 환경상 미실행 → 라이브로 보완.)