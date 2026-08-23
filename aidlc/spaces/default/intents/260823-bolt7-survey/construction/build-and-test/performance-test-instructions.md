# Performance Test Instructions — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(quality 리드). 로컬 MVP — 파일럿 가이드, 정식 부하 이월. -->

## 범위
로컬 MVP·설계 전용 후속 구현. 정식 부하는 범위 밖.

## 관찰 포인트 (Bolt 7)
- **응답 제출**: 문항 수만큼 upsert(모임당 소수 문항) — 경미. `unique(question_id,mentee_id)`로 멱등.
- **열람 쿼리**: survey_answer/feedback 모두 meeting_id 인덱스 활용. FeedbackViewPage의 멘티별 응답 조회는 N+1 가능 — 파일럿 규모 무시 가능(대량 시 배치 read 검토).

## 파일럿 확인
- 소규모 시드에서 응답 제출/열람 체감 지연 없음 확인.
- 정식 부하·N+1 최적화는 후속(operation/performance-validation) 이월.
