# Performance Test Instructions — Bolt 6 Session/Attendance (learnKK)

<!-- build-and-test 산출물. 입력: construction/bolt6-session/code-generation/code-generation-plan.md·code-summary.md + U5 nfr-requirements/performance-requirements.md. 로컬 파일럿 — 경량. -->

## 범위·근거
- U5 nfr-requirements(performance)는 로컬 파일럿 수준. 대규모 부하 목표 없음. 스케줄러리스(ADR-005)라 백그라운드 잡 부하 없음 — checkIn 요청 시점 시간창 비교만.
- 이 스코프는 로컬 전용이며 operation phase(performance-validation 포함) 미실행 — 여기서는 지침만 정의한다.

## 점검 항목
- `computeCompletion(meetingId)`: 참여자 N명 × 세션 S개 집계. `countAttendedSessions`가 attendance⋈meeting_session 조인으로 멘티당 1쿼리 — N 증가 시 쿼리 수 선형. 대규모 시 배치 집계(GROUP BY)로 최적화 여지(파일럿 범위 밖, 이월).
- 인덱스 확인: `idx_meeting_session_meeting(meeting_id)`, `attendance(session_id,mentee_id)` unique, `idx_attendance_mentee`.

## 측정 방법(선택)
- 로컬에서 세션 S=수십·참여자 N=수십 규모로 computeCompletion 응답시간 관측(예: curl `-w %{time_total}`). 회귀 기준선만 기록.
