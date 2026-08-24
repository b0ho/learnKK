# Code Generation — Observation Diary (Bolt 6 Session/Attendance)

<!-- 오케스트레이터 유지. 손으로 편집하지 않음. code-generation 스테이지 관측 로그. -->

## Interpretations

- 2026-08-23T15:36:00Z — Bolt 6(Session/Attendance, U5) 대상. Bolt 1·2·3 패턴 동일: memory_path {unit-name}을 bolt 레벨(`construction/bolt6-session/`)로 해석. 설계는 설계 intent(`260731-learnkk-crew`)의 `construction/U5-session-attendance/`에서 상속.
- 2026-08-23T15:36:00Z — Bolt 6 범위(bolt-plan): 세션 일정 지정·변경(복수)·멘티 팝업 출석(시간창)·출석율(세션 기준)·80% 자동판정·④ 확정 판정. Brownfield — Bolt 1/2/3 코드에 신규 session 모듈 추가 + 기존 시임(SessionCompletionGate) 배선.

## Deviations

- (실행 중 기록)

## Tradeoffs

- 2026-08-23T15:36:00Z — [table-name] 도메인 설계는 테이블을 `session`으로 부르나 auth 토큰 테이블(V2)이 `sessions`(복수)로 이미 존재. 충돌 회피를 위해 U5 세션 테이블은 `meeting_session`으로 명명(enrollment의 단수 관례 계승). JPA 엔티티는 `com.learnkk.session.entity.Session`(패키지 격리)로 두고 `@Table(name="meeting_session")` 매핑.
- 2026-08-23T15:36:00Z — [scheduler-less] ADR-005: 백그라운드 잡 없이 checkIn 요청 시점 `now ∈ [scheduledAt, scheduledAt+checkInWindowMinutes]` 판정. 팝업 트리거는 FE 클라이언트 타이머, 서버는 시간창 검증만.
- 2026-08-23T15:36:00Z — [completion-math] 80% 판정은 정수 연산 `a*100 >= 80*S`(부동소수 회피). S>0 가드로 S=0 후보 판정 보류(리뷰 검증). getMyAttendance rate는 S=0시 0 반환(리뷰 S1 반영, 0나눗셈 회피).
- 2026-08-23T15:36:00Z — [participant-read-port] 리뷰 S3 반영: computeCompletion/checkIn의 참여자 판정은 U4에 신규 무권한 read 포트(`listActiveMenteeIds(meetingId)`, `isActiveParticipant(meetingId, menteeId)`)를 추가해 서비스 레이어 경유(ADR-007 R-1). EnrollmentRepository 직접 참조(모듈 경계 침범) 지양.
- 2026-08-23T15:36:00Z — [seam-wiring] Bolt 2가 남긴 `NoSessionsCompletionGate` 스텁(항상 true)을 실제 `SessionService` read 기반 `SessionBackedCompletionGate`로 교체(ADR-007 R-2, U3→U5 read). 상태 쓰기(COMPLETED)는 U3 소유 유지. 세션 미등록(S=0) 시 vacuous-true(완료 허용) — 스텁 동작 무회귀.
- 2026-08-23T15:36:00Z — [approve-guard-order] 리뷰 S2 반영: approveMenteeCompletion은 COMPLETED(중복) 검사를 COMPLETION_CANDIDATE 미충족 검사보다 먼저 수행(사문화 방지).

## Open questions

- 2026-08-23T15:36:00Z — [env] 시간창·멱등·80% 경계 통합 테스트는 Testcontainers 필요 — 이 환경(Windows/Rancher JNA) 미실행 예상. 라이브 API E2E로 보완 검증 예정(Bolt 1/2/3 동일).
- 2026-08-23T15:36:00Z — [assumption] checkInWindowMinutes 기본 120분, 세션 활성 시점=모임 IN_PROGRESS, ④/③ 순서 무관(파일럿). team 미확정 항목은 설계 [assumption] 따름.
- 2026-08-23T15:36:00Z — [note] ci-pipeline·operation은 project.md Scope Override로 build-and-test(3.6) 후 SKIP 예정.

- 2026-08-24T02:45:00Z — [defect-found-in-e2e] 라이브 E2E(실행 앱 + clone-3 Postgres)가 단위/슬라이스 테스트가 놓친 컨텍스트 부팅 결함 3건을 잡음: (1) Spring Data 빈 이름 충돌 `sessionRepository`(auth vs U5) → `MeetingSessionRepository`로 리네임; (2) `AttendanceRepository.countAttendedSessions` JPQL `FROM ... Session s`가 auth 토큰 엔티티로 해석(Long vs String) → `MeetingSession`으로 수정; (3) 엔티티명은 이미 격리됨. **교훈: @WebMvcTest/@Mock 슬라이스는 전체 ApplicationContext·JPA 부팅·JPQL 검증을 커버하지 못한다 — 신규 모듈은 최소 1개의 부팅형 검증(통합 or 라이브 E2E)이 필요.**
- 2026-08-24T02:45:00Z — [defect-class] 동일 simple name(Session/SessionRepository)을 다른 패키지에 두면 (a) Spring Data 빈 이름, (b) JPA 엔티티명, (c) JPQL 참조가 모두 전역 네임스페이스에서 충돌한다. 신규 도메인 타입은 기존 도메인(특히 auth)과 simple name이 겹치지 않게 접두(Meeting*)를 붙인다.
- 2026-08-24T02:45:00Z — [e2e-verified] 44/44 통과. 시간창·멱등·80% 경계(a*100>=80*S)·S=0·④ 가드·완료 게이트 seam 전부 실 DB로 입증.