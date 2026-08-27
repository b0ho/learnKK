# Unit Test Instructions — apply-button-state

## Backend
```
cd backend && ./gradlew test --tests "com.learnkk.meeting.web.MeetingControllerTest" \
  --tests "com.learnkk.enrollment.service.EnrollmentServiceTest" \
  --tests "com.learnkk.contract.OpenApiContractTest"
```
회귀 포인트: 모집 목록 `enrolledCount`/`full` 보강, `activeCountsByMeeting` 집계, MeetingSummary 계약.

## Frontend
```
cd frontend && npx vitest run src/features/meetings/MeetingListPage.test.tsx
```
회귀 포인트: 로드 시 기존 신청 → "신청완료" 비활성 / `full:true` → "마감" 배지 + 비활성 버튼.
