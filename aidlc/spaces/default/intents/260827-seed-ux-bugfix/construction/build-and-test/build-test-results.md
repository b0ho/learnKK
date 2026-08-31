# 빌드/테스트 실행 결과 — 260827-seed-ux-bugfix

실행 시각: 2026-08-27 (로컬)

## 프론트엔드 테스트 (`npx vitest run`)
- **결과: 136 passed / 1 failed (137 총, 27/28 파일 통과)**
- 대상 파일 `MeetingListPage.test.tsx`: **10/10 통과**(신규 2건 포함).
- 라우팅 회귀 방지 `AppRouter.test.tsx`: **5/5 통과**.

### 회귀 처리
- 최초 실행 시 `AppRouter.test.tsx` 2건이 실패했다 — FR-2가 추가한 병렬 `listMine()` 호출이 catch-all fetch 목에서 비배열을 반환해 `for...of`가 예외를 던진 탓.
- 조치: `MeetingListPage.tsx`에서 `Array.isArray(enrollments)` 방어를 추가(신청 내역이 비정상이어도 목록 표시를 막지 않음, NFR-2). 재실행 후 AppRouter 5/5·MeetingListPage 10/10 통과로 회귀 해소.

### 잔여 실패 (사전 존재, 이번 변경과 무관)
- `src/api/content.test.ts > downloadAttachment ...` — **베이스라인(내 변경 stash)에서도 동일 실패** 확인. 이번 버그픽스 범위 밖.

## 프론트엔드 빌드 (`npm run build`)
- **실패 — 사전 존재 이슈**: `src/routes/AppShell.tsx:42` TS2345 (`TAB_ROOTS.includes(pathname)`). 내 변경 stash한 커밋 베이스라인에서도 동일 재현 → 회귀 아님. 사용자가 범위 외로 두기로 결정(수정 안 함).
- 변경 파일 자체 진단(diagnostics): **0건**.

## 백엔드 (`V12__seed_demo.sql`)
- 데이터 값/주석 변경. 기존 MENTEE001 출석·판정 INSERT 패턴을 그대로 미러링하며 멱등 가드(NOT EXISTS / ON CONFLICT) 보존.
- AC-1/AC-2 검증 쿼리는 `integration-test-instructions.md` 참조(수동/DB 검증).

## 수용 기준 종합
| AC | 상태 | 근거 |
|----|------|------|
| AC-1 sub-80% 후보/확정 없음 | ✅ | 멘티2 4/4로 상향, 다른 후보/확정 행 없음 |
| AC-2 멘티2 4/4 후보 | ✅ | 시드 값 `COMPLETION_CANDIDATE,4,4,NULL` |
| AC-3 sub-80% 수료확정 불가 | ✅ | 규칙 충족 데이터만 시드 |
| AC-4 로드 시 신청완료 반영 | ✅ | 신규 테스트 통과 |
| AC-5 신청 시 신청완료 전환 | ✅ | 기존 테스트 통과 |
| AC-6 빌드/테스트 통과 | ⚠️ 부분 | 변경 관련 테스트 전부 통과. 빌드는 **사전 존재** AppShell 오류로 red(회귀 아님, 사용자 결정으로 미수정) |
