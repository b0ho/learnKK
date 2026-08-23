# Performance Test Instructions — Bolt 5 Messaging (Pilot Guide)

<!-- 상류: bolt5-messaging code-generation-plan.md · code-summary.md · learnkk-crew U7 nfr-requirements/performance-requirements.md. Standard 전략 · 파일럿 로컬 MVP → 경량 가이드(정식 부하 이월). -->

## 관심 지점 (Hotspots)

- **미확인 카운트 폴링**: `GET /api/messages/unread-count`가 FE에서 30초 간격 폴링 → 단일 집계 쿼리(`countUnreadForUser`, 인덱스 `idx_message_unread`).
- **스레드 목록 N+1**: `listThreads`가 스레드당 partner/last-message/unread 조회. 파일럿 규모 허용, 스레드 수 증가 시 배치 조회로 최적화.
- **인덱스**: `idx_message_thread(thread_id, created_at)`(전문), `idx_message_unread(thread_id, sender_id, read_at)`(미확인), `idx_message_thread_a/_b`(내 스레드).

## 측정 방법 (권장, 이월)

- 로컬 스모크: 실행 앱에 수백 메시지 시드 후 `unread-count`·`threads`·`threads/{id}` 응답시간 관측(목표 p95 < 300ms, 파일럿).
- 정식 부하/회귀 벤치는 CI·operation 스코프(이번 SKIP) — Bolt 3와 동일하게 이월.

## 현재 상태

- 전용 부하 테스트 미실행(파일럿). 쿼리는 인덱스 뒷받침. 폴링 주기(30초)는 FE 설정으로 조정 가능.
