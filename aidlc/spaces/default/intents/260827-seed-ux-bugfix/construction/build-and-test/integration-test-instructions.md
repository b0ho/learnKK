# 통합 테스트 지침 — 260827-seed-ux-bugfix

## FR-1 시드 정합성 (수동/DB 검증)
깨끗한 DB에 마이그레이션 적용 후 다음 쿼리로 규칙 위반 행이 없음을 확인한다(AC-1):

```sql
SELECT * FROM mentee_completion
WHERE status IN ('COMPLETION_CANDIDATE','COMPLETED')
  AND attended_count * 100 < 80 * total_scheduled;
-- 기대: 0 rows
```

CS 스터디 멘티2가 4/4 후보인지 확인(AC-2):

```sql
SELECT mc.status, mc.attended_count, mc.total_scheduled, mc.approved_at
FROM mentee_completion mc
JOIN meetings m ON m.id = mc.meeting_id
JOIN users u ON u.id = mc.mentee_id
WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE002';
-- 기대: COMPLETION_CANDIDATE, 4, 4, NULL
```

## FR-2 (E2E, 선택)
멘티로 로그인 → 모집중 목록 진입 → 이미 신청한 모임이 클릭 없이 "신청완료"/비활성으로 표시되는지 확인.
