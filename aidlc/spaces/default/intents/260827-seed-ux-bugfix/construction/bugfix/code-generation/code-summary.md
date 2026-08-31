# 코드 요약 — 260827-seed-ux-bugfix

## 변경 파일
| 파일 | 변경 | 요구 |
|------|------|------|
| `backend/src/main/resources/db/migration/V12__seed_demo.sql` | CS 스터디 멘티2를 4/4 진짜 수료 후보로 시드 | FR-1 |
| `frontend/src/features/meetings/MeetingListPage.tsx` | 목록 로드 시 멘티 신청 상태 반영 | FR-2 |
| `frontend/src/features/meetings/MeetingListPage.test.tsx` | fetch 라우팅 + 로드시 신청상태 테스트 2건 추가 | FR-2 검증 |

## FR-1 — 시드 80% 규칙 위반 수정
- 출석: 멘티2가 `s.week IN (1,2,3)`(3건)만 출석하던 것을, 멘티1과 동일하게 CS 스터디 **4개 세션 전체** 출석하도록 `s.week` 필터를 제거.
- 판정: `mentee_completion` 멘티2 행을 `COMPLETION_CANDIDATE, attended_count=4, total_scheduled=4, approved_at=NULL` 로 변경(기존 3/4). 규칙 `a*100 >= 80*S` → `400 >= 320` 참.
- 주석 갱신(3/4 → 4/4) 및 규칙 충족 설명 주석 추가.
- 멘티1(4/4 CONFIRMED)·멘티4(1/4 NOT_COMPLETED)는 불변. 결과적으로 확정 vs 후보가 **판정 상태만** 다른 데모가 됨.
- 애플리케이션 로직 무변경(`CompletionService.applyJudgement` 정상).

### 수용 기준
- AC-1: 신규 시드에 sub-80% 인 `COMPLETION_CANDIDATE`/`COMPLETED` 행 없음 ✅
- AC-2: 멘티2 출석 4건 + `4/4` 후보 + `approved_at=NULL` ✅
- AC-3: 관리/운영현황에서 sub-80% "수료확정" 불가 ✅(후보 자체가 규칙 충족)

## FR-2 — 목록 로드 시 신청 상태 반영
- 로드 effect를 `Promise.all([listRecruiting, enrollmentsPromise])` 로 변경.
- `role === 'MENTEE'` 일 때만 `enrollmentsApi.listMine()` 호출, 아니면 빈 배열.
- `status === 'APPLIED'` 인 항목의 `meetingId` 로 `applied` 맵 초기화 → 버튼이 로드 시점부터 비활성/"신청완료".
- 신청 내역 조회 실패는 `.catch(() => [])` 로 흡수 → 목록 표시를 막지 않음.
- effect 의존성 `[]` → `[role]` (role 확정 시 재조회).
- 신청 직후 "신청완료" 전환 등 기존 동작 불변.

### 수용 기준
- AC-4: 이미 신청한 모임이 로드 시점에 비활성/"신청완료" — 신규 테스트로 검증 ✅
- AC-5: 미신청 모임 신청 시 "신청완료" 전환 — 기존 테스트 통과 ✅

## 검증 결과
- `vitest run MeetingListPage.test.tsx`: **10/10 통과**(신규 2건 포함).
- 변경 파일 진단: 0건.
- 백엔드 시드 변경은 기존 멘티1 패턴 미러링(문법/멱등 가드 보존).

## 사전 존재 이슈(범위 외, 회귀 아님)
- `frontend/src/routes/AppShell.tsx:42` `TAB_ROOTS.includes(pathname)` TS2345 → `npm run build` 실패. 내 변경 stash한 베이스라인에서도 동일 재현 → 이번 작업 무관. AC-6(빌드 통과)은 이 사전 이슈 때문에 현재 red. 별도 수정 필요 여부는 사용자 판단.
