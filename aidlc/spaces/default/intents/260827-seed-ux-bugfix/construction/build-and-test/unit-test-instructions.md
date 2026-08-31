# 단위 테스트 지침 — 260827-seed-ux-bugfix

## 프론트엔드 (vitest)
- 전체: `cd frontend && npx vitest run`
- 대상만: `npx vitest run src/features/meetings/MeetingListPage.test.tsx`

### FR-2 커버리지 (MeetingListPage.test.tsx)
- 로드 시 이미 신청한 모임 → 버튼 비활성 + "신청완료" (신규, AC-4)
- 취소(CANCELLED) 신청건은 신청완료로 표시하지 않음 (신규)
- 신청 성공 시 "신청완료" 전환 (AC-5)
- 409 ENROLLMENT_FULL → 한국어 에러 메시지
- 빈/에러/멘토/멘티 렌더 케이스

## 백엔드
- 전체: `cd backend && ./gradlew test`
- FR-1(시드)은 데이터 값 변경으로 단위 테스트 대상 아님. AC-1/AC-2는 SQL 구조로 보장(아래 결과 문서 참조).
