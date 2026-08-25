# Unit Test Instructions — ux-bugfixes-2

## Frontend (Vitest + RTL)
```bash
cd frontend
CI=true npx vitest run              # 전체
npx vitest run src/features/meetings/MyLearningPage.test.tsx   # 개별
```
- 변경 영향: AppShell(FR-1/3), AppRouter(FR-2), MyLearningPage(FR-2/5/7), FeedbackViewPage(FR-4), ThreadView/useUnreadCount(FR-8), MeetingListPage(FR-9), AdminApprovalPage(FR-10). 기존 스위트 그대로 green 유지.

## Backend (JUnit5 + Mockito)
```bash
cd backend
./gradlew test --tests "com.learnkk.session.service.AttendanceServiceTest"       # FR-5
./gradlew test --tests "com.learnkk.meeting.service.MeetingApprovalServiceTest"  # FR-6
./gradlew test --tests "com.learnkk.contract.OpenApiContractTest"                # 계약
```
- FR-5: AttendanceServiceTest가 `findAttendedSessionIds` stub + `attendedSessionIds` 검증으로 갱신.
- FR-6: MeetingApprovalServiceTest가 세션 미종료여도 완료(COMPLETED)됨을 검증.
