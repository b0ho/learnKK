# 코드 생성 계획 — 260827-seed-ux-bugfix

- **유닛**: bugfix (단일 암묵 유닛, units-generation 미실행)
- **스코프**: bugfix / minimal depth
- **리뷰어**: aidlc-architecture-reviewer-agent

## 단계

### FR-1 — 시드 80% 규칙 위반 수정 (`V12__seed_demo.sql`)
- [x] 1. CS 스터디 멘티2 출석 INSERT를 주차 `IN (1,2,3)`(3건)에서 전체 4주로 변경(멘티1과 동일하게 `s.week` 필터 제거).
- [x] 2. 관련 출석 주석 "멘티2 = 3/4(수료 후보)" → "멘티2 = 4/4(수료 후보)".
- [x] 3. `mentee_completion` 멘티2 행 `COMPLETION_CANDIDATE, 3, 4` → `COMPLETION_CANDIDATE, 4, 4`.
- [x] 4. 수료 후보 행에 규칙 충족 설명 주석 추가.

### FR-2 — 모집중 목록 신청 상태 로드 반영 (`MeetingListPage.tsx`)
- [x] 5. 로드 effect에서 멘티(`role === 'MENTEE'`)일 때 `enrollmentsApi.listMine()`을 목록 조회와 함께 호출.
- [x] 6. 반환 중 `status === 'APPLIED'`인 항목의 `meetingId`로 `applied` 맵을 초기화(`setApplied`).
- [x] 7. 비멘티/조회 실패 시 기존 목록 로드 동작 불변(신청상태 조회 실패는 목록 표시를 막지 않음 — `.catch(() => [])`).

### 검증
- [x] 8. 프론트엔드 테스트 실행 — `MeetingListPage.test.tsx` 10건 통과(신규 2건: 로드 시 신청완료 반영, 취소건 미반영). 변경 파일 진단 0건.
- [x] 9. 백엔드: 시드 SQL 변경은 기존 멘티1 출석/판정 패턴을 그대로 미러링 — 문법/멱등 가드 보존 확인.
- [x] 10. `code-summary.md` 작성.

## 원칙
- 시드는 in-place 수정(새 마이그레이션 없음), 멱등 가드 보존.
- 애플리케이션 수료 로직 무변경. FR-2는 프론트엔드 전용.

## 알려진 사전 존재 이슈(내 변경과 무관)
- `frontend/src/routes/AppShell.tsx:42` — `TAB_ROOTS.includes(pathname)` TS2345 타입 오류로 `npm run build`(tsc -b)가 실패한다. 내 프론트엔드 변경을 stash한 커밋 베이스라인에서도 동일하게 재현됨 → 이번 버그픽스가 유발한 회귀가 아님. 범위 외이므로 사용자 판단에 맡긴다.
