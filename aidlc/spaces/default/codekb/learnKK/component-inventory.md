# 컴포넌트 인벤토리 — learnKK

## 백엔드 서비스/컴포넌트
| 컴포넌트 | 책임 | 주요 의존 |
|---|---|---|
| `MeetingService` | 모임 생성·조회·목록(recruiting/mine) | MeetingRepository |
| `MeetingApprovalService` | 상태전이 T1~T6(승인/반려/모집확정/시작/완료) | MeetingRepository.transitionStatus, SessionCompletionGate |
| `SurveyTemplateService` | 사전설문 문항 upsert/조회, lock 판정 | SurveyQuestionRepository |
| `EnrollmentService` | 신청/취소, 참여자 read 포트(isActiveParticipant 등) | EnrollmentRepository, MeetingService |
| `SessionService` | 세션 생성/일정변경/목록, 종료 게이트(allScheduledSessionsEnded) | MeetingSessionRepository, MeetingService |
| `AttendanceService` | 시간창 체크인(멱등), 출석 요약 | AttendanceRepository |
| `CompletionService` | 80% 자동 판정, ④ 확정 | MenteeCompletionRepository, EnrollmentService |
| `SessionBackedCompletionGate` | T6 완료 게이트 구현(세션 종료 여부) | SessionService |
| `SessionAuthInterceptor` | 경로별 인증 필요 판정 | 정규식 매칭 |

## 프론트엔드 주요 컴포넌트 (이번 버그픽스 대상 ★)
| 컴포넌트 | 책임 | 버그픽스 |
|---|---|---|
| `SurveyBuilder` ★ | 사전설문 문항 추가/삭제/유형/선택지 편집 | #1 쉼표 입력 |
| `AdminApprovalPage` ★ | 관리자 모임 승인·수료 판정 | #2 리스트화 #3 영역별 #5 되돌리기 #6 확인 |
| `AppShell` ★ | 하단 탭 네비 | #4 관리자 '관리' 탭 |
| `MyLearningPage`(MentorHub/MenteeLearning/MentorSessions/MenteeSessions) ★ | 운영 허브·내 러닝·세션 관리/출석 | #7 세션 삭제 #8 완료 #9 자료실 강조 #10 문항 수정 #12 재신청 |
| `FeedbackViewPage` ★ | 피드백+사전설문 열람 | #11 분리 |
| `MeetingCreatePage` | 모임 개설 + SurveyBuilder | #1 소비처 |

## API 클라이언트 (FE `src/api`)
`meetingsApi, enrollmentsApi, sessionsApi, adminApi, surveyApi, feedbackApi, contentApi, messagesApi, usersApi, authApi` — 이번 버그픽스로 `adminApi`(listByStatus/revert), `sessionsApi`(delete/complete) 확장 예정.
