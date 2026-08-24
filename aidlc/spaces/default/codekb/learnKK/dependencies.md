# 의존성 — learnKK

## 외부 의존성
### 프론트엔드 (package.json)
- react, react-dom, react-router-dom (런타임 라우팅/UI)
- @radix-ui/*, tailwindcss, class-variance-authority, clsx, lucide-react (UI)
- vite, typescript, vitest, @testing-library/react·user-event·jest-dom (빌드/테스트)
- js-yaml (계약 검증 유틸)

### 백엔드 (build.gradle)
- spring-boot-starter-web, -data-jpa, -validation
- postgresql (JDBC), flyway-core
- spring security crypto(bcrypt) 또는 자체 해시
- junit-jupiter, mockito, spring-boot-starter-test, testcontainers(postgresql)

### 인프라
- docker-compose (PostgreSQL 16)

## 내부 크로스모듈 의존 (ADR-007 read 포트, 순환 없음)
- U5 Session → U3 `MeetingService.getMeeting`(상태·소유 멘토), U4 `EnrollmentService.listActiveMenteeIds`/`isActiveParticipant`.
- U6 Content → U4 `EnrollmentService.isParticipant`.
- U7 Messaging → U3 `MeetingService.meetingIdsOwnedBy`/`mentorIdsForMeetings`, U4 `EnrollmentService.activeMeetingIdsForMentee` 등.
- U8 Survey → U3 `SurveyTemplateService.getQuestions`, U4 `EnrollmentService.isActiveParticipant`.
- U3 완료 게이트 → `SessionCompletionGate` 인터페이스 뒤로 U5 `SessionBackedCompletionGate`.

## 마이그레이션 순서 (Flyway)
V1 baseline → V2 auth → V3 meeting → V4 enrollment → V5 content → V6 messaging → V7 session → V8 survey_feedback. (bolt4~7 통합 시 재번호 완료; 이번 버그픽스에서 세션 완료 플래그 추가 시 V9 예정.)
