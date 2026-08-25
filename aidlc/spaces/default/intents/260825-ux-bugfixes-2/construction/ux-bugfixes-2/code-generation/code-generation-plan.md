# Code Generation Plan — ux-bugfixes-2

requirements.md(FR-1~FR-11)를 단일 암묵 유닛으로 구현한다. 신규 도메인 없음 — 기존 U3~U9·내비게이션·시드 보정.

## 구현 매핑

| FR | 계층 | 파일 | 방법 |
|----|------|------|------|
| FR-1 활성 탭 재클릭 재조회 | FE | routes/AppShell.tsx | 활성 탭 클릭 시 `preventDefault` + 탭 루트 이동 + `reloadKey` 증가로 `<Outlet>` 래퍼 key 변경 → 리마운트 |
| FR-2 내 러닝 탭 유지 | FE | routes/AppRouter.tsx, routes/paths.ts, features/meetings/MyLearningPage.tsx | `/my-learning/meetings/:id/{content,feedback,feedback-view,survey-answer,questions-edit}` 라우트 추가(동일 컴포넌트 재사용), 카드 링크를 my-learning 스코프 경로로 |
| FR-3 우하단 이전 버튼 | FE | routes/AppShell.tsx | 탭 루트가 아니면 플로팅 Button `navigate(-1)` |
| FR-4 응답+문항 표시 | FE | features/survey/FeedbackViewPage.tsx | `getQuestions`로 questionId→text 매핑, 응답에 문항 텍스트 결합 |
| FR-5 출석완료 유지 | BE+FE | session/dto/AttendanceSummaryResponse, AttendanceRepository, AttendanceService; api/types, MyLearningPage | 응답에 `attendedSessionIds` 추가(`findAttendedSessionIds`), FE가 로드 시 checkedIn 시드 |
| FR-6 세션 미종료 완료 | BE | meeting/service/MeetingApprovalService | `completeMeeting`에서 `sessionCompletionGate` 게이트 제거 |
| FR-7 멘토 수료 판정 버튼 | FE | features/meetings/MyLearningPage.tsx | MentorCompletionPanel(compute/list, ④확정은 관리자 유지) |
| FR-8 쪽지 뱃지 갱신 | FE | features/messaging/useUnreadCount.ts, ThreadView.tsx | `notifyMessagesRead()` 이벤트 → 뱃지 즉시 refetch |
| FR-9 개설승인 버튼 제거 | FE | features/meetings/MeetingListPage.tsx | ADMIN용 open-admin-queue 버튼 삭제 |
| FR-10 단계 카운트 제거 | FE | features/meetings/AdminApprovalPage.tsx | 섹션 헤더 `(n)` 제거 |
| FR-11 시드 | BE | db/migration/V12__seed_demo.sql | 전 상태 모임·참여/출석/수료/설문/피드백, 자연키 멱등 |

## 계약 변경
- `AttendanceSummaryResponse`에 `attendedSessionIds: int64[]` 추가 → contracts/openapi.yaml 반영, OpenApiContractTest 그대로 통과(2-arg `of` 유지).

## 테스트 영향
- 갱신: AttendanceServiceTest(findAttendedSessionIds stub), MeetingApprovalServiceTest(완료 게이트 제거).
- 기존 FE 스위트 그대로 통과.
