# 단위 테스트 지침 — learnKK ux-bugfixes (Minimal)

## 실행 방법
- 백엔드(단위/웹/계약, Docker 불필요):
  `cd backend && ./gradlew test --tests "com.learnkk.*ServiceTest" --tests "com.learnkk.*ControllerTest" --tests "com.learnkk.contract.OpenApiContractTest"`
- 프론트: `cd frontend && ./node_modules/.bin/vitest run`

## 커버리지(요구사항 구동, FR별 1+ 회귀)
- **FR-1** SurveyBuilder: CHOICE 선택지에 "초급, 중급" 입력 시 쉼표 유지(`SurveyBuilder.test.tsx`).
- **FR-2/3/5/6** AdminApprovalPage: 상태별 목록 렌더, 승인 전 확인 다이얼로그, 반려 사유 검증, 되돌리기 액션, 완료 패널(`AdminApprovalPage.test.tsx`).
- **FR-5** MeetingApprovalService.revert: 4개 전진 역전이 + 불가상태(PENDING/REJECTED) 409 + 비관리자 403(`MeetingApprovalServiceTest`).
- **FR-7/8** SessionService: deleteSession(소유자/출석 CASCADE), completeSession, 완료된 미래 세션의 종료 판정(`SessionServiceTest`).
- **FR-11** FeedbackViewPage: 피드백/사전설문 별도 섹션, 사전설문 섹션의 피드백 독립성(`FeedbackViewPage.test.tsx`).
- **FR-12** EnrollmentService: 취소 후 재활성화, APPLIED 중복 409, 정원 마감 409(`EnrollmentServiceTest`).
- 계약: `MeetingSessionResponse.completed` 스키마/DTO 일치(`OpenApiContractTest`).

## 기대치
- 백엔드 단위/웹/계약 전부 green. 프론트 전체 파일 green. Minimal 전략이라 컴포넌트당 소수의 요구사항 구동 테스트를 유지한다.
