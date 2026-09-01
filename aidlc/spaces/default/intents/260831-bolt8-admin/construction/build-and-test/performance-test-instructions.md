# Performance Test Instructions — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(quality 리드). 로컬 MVP — 파일럿 가이드, 정식 부하 이월. -->

## 범위
로컬 MVP·파일럿 규모. 정식 부하는 범위 밖.

## 관찰 포인트 (Bolt 8)
- **행당 read 조합(N+1)**: 모임 1행당 세션 목록 + 멘티 목록 + 출석 카운트 + 수료 목록 + 멘토 조회(≈5 쿼리). 페이지 size [1,100] 클램프로 상한 고정 — 파일럿(수십 모임) 무시 가능.
- **대량 전환 기준**: 모임 수백 개 이상 운영 시 meeting_id IN 배치 read(세션/출석/수료 GROUP BY meeting_id) 또는 집계 뷰로 전환 검토 — 서비스 시그니처 불변으로 교체 가능(read 전용).
- **인덱스**: attendance/meeting_session/mentee_completion 모두 meeting_id 인덱스 기활용(기존 마이그레이션) — 신규 인덱스 불요.

## 파일럿 측정
- 관리자 토큰으로 `GET /api/admin/monitoring/meetings?size=100` p95 응답시간 기록(목표: 로컬 <500ms). Hibernate SQL 로그로 행당 쿼리 수 확인.
