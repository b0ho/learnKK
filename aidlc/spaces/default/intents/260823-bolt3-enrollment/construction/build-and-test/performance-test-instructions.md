# Performance Test Instructions — Bolt 3 Enrollment (learnKK)

<!-- build-and-test 산출물(quality 리드). 로컬 MVP — 파일럿 가이드, 정식 부하 이월. -->

## 범위
로컬 MVP·설계 전용 후속 구현이라 정식 부하는 범위 밖. 파일럿 확인용 가이드.

## 관찰 포인트 (Bolt 3)
- **신청 동시성**: apply는 `pg_advisory_xact_lock(meetingId)`로 모임 단위 직렬화. 락 경합은 동일 모임 신청에 국한(서로 다른 모임은 병렬). 잔여 1석 폭주 시에도 정합성 우선(무결성 > 처리량).
- **목록 쿼리**: `idx_enrollment_meeting_status(meeting_id,status)`(신청자·정원 카운트), `idx_enrollment_mentee(mentee_id)`(내 신청).

## 파일럿 확인
- 소규모 시드에서 신청/취소/목록 응답 체감 지연 없음 확인. 동일 모임 고동시성 신청 시 락 대기 시간은 파일럿 규모에서 무시 가능.
- 정식 부하(동시 사용자·처리량 SLO)는 후속 워크플로우(operation/performance-validation)로 이월.
