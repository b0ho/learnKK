# Code Summary — Bolt 5 Messaging (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 5 = U7 Messaging. Brownfield: Bolt 1~3 코드에 신규 messaging 모듈 추가 + 시임 배선. 상속 설계: 260731-learnkk-crew intent U7. -->

## 무엇을 만들었나 (Definition of Done 달성)

> B5 U7 DoD: 쪽지 스레드·미확인 뱃지·권한 경계. 가설: 권한 경계(멘토=자기 모임 멘티, 관리자=전원)가 403으로 강제된다.

- **권한 경계 서버 강제(핵심 가설 실증)**: `MessageService.canMessage(sender, recipient)` — ADMIN↔전원, MENTOR→자기 모임 활성(APPLIED) 멘티, MENTEE→신청 모임 멘토. 위반 시 403 `MESSAGING_FORBIDDEN`. 자기발신 400 `MESSAGING_SELF`, 빈 본문 400 `MESSAGING_EMPTY_BODY`, 상대 없음 404 `MESSAGING_RECIPIENT_NOT_FOUND`.
- **1:1 스레드**: 참여자 쌍 정규화(min/max) + `UNIQUE(participant_a, participant_b)` → 대화당 스레드 1개. 양방향 발신이 동일 스레드에 수렴.
- **미확인 뱃지**: `GET /api/messages/unread-count`(수신·미확인 합산). FE 하단탭 "쪽지"에 30초 폴링 뱃지.
- **열람 확인 처리**: `getThread`가 수신 메시지를 멱등 확인 처리(`read_at IS NULL`만 UPDATE).

## 엔드포인트 (계약 #1, openapi 0.4.0-bolt5)

- `POST /api/messages` — 발신(201). `{recipientId, body}`.
- `GET /api/messages/threads` — 내 스레드 목록(partner·마지막 메시지·미확인 수).
- `GET /api/messages/threads/{id}` — 스레드 전문(page/size), 열람 시 확인 처리. 비참여자 403.
- `GET /api/messages/unread-count` — 미확인 총합(뱃지).
- `GET /api/messages/recipients` — 발신 가능 상대(FE 선택 보조; 서버 send가 최종 권위).

## 산출물 (파일)

**Backend (신규)**
- `db/migration/V5__messaging.sql` — `message_thread`, `message` + 인덱스.
- `messaging/entity/{MessageThread, Message}.java` — FK by id, 정규화 팩토리, 멱등 `markRead`.
- `messaging/repository/{MessageThreadRepository, MessageRepository}.java` — 쌍 조회, 참여자 스레드, 멱등 확인 UPDATE, 미확인 합산.
- `messaging/service/MessageService.java` — send/listThreads/getThread/unreadCount/listRecipients + canMessage.
- `messaging/dto/{SendMessageRequest, MessageResponse, ThreadSummaryResponse, UnreadCountResponse, RecipientResponse}.java`.
- `messaging/web/MessageController.java`.

**Backend (수정 — 시임/크로스모듈)**
- `kernel/error/ErrorCodes.java` — MESSAGING_* 5종.
- `meeting/…/MeetingService.java` + `MeetingRepository.java` — `meetingIdsOwnedBy`, `mentorIdsForMeetings`(read-only, ADR-007).
- `enrollment/…/EnrollmentService.java` + `EnrollmentRepository.java` — `isActivelyEnrolledInAnyOf`, `activeMeetingIdsForMentee`, `activeMenteeIdsForMeetings`.
- `auth/…/UserRepository.java` — `findByRole`, `findByIdNot`.
- `auth/web/SessionAuthInterceptor.java` — `/api/messages/**` 인증 보호.

**Frontend (신규)**
- `api/messages.ts` — `messagesApi`.
- `features/messaging/{MessagesPage, ThreadView, NewMessageDialog}.tsx`, `useUnreadCount.ts`, `formatTime.ts`.

**Frontend (수정)**
- `api/{types.ts, errors.ts, index.ts}` — 메시징 타입·에러 메시지·배럴.
- `routes/{paths.ts, AppRouter.tsx, AppShell.tsx}` — 라우트 + "쪽지" 탭 + 미확인 뱃지.

**계약/테스트**
- `contracts/openapi.yaml` → 0.4.0-bolt5(paths 5개 + schemas 6개 + messaging 태그).
- BE: `MessageServiceTest`, `MessageControllerTest`, `integration/MessageIntegrationTest`, `OpenApiContractTest`(+4 메시징 스키마).
- FE: `MessagesPage.test.tsx`, `ThreadView.test.tsx`, `AppShell.test.tsx`, `AppRouter.test.tsx`(unread-count 폴링 반영).

## 검증 결과

- **BE**: messaging(서비스+웹) + contract + meeting/enrollment/auth/kernel 테스트 통과. `spotlessCheck`·`checkstyleMain`·`checkstyleTest`·`compileJava` 통과(sensors: linter, type-check).
- **FE**: 전체 91 테스트 통과(19 파일). `eslint` 0건, `prettier --check` 통과, `tsc -b` 0 오류.
- **미실행**: `MessageIntegrationTest`(Testcontainers) — 이 환경 Windows/Rancher JNA 제약으로 Bolt 1~3과 동일하게 로컬 미실행. 코드 결함 아님. build-and-test에서 재확인 대상.

## 결정·이탈·이월

- **결정**: `listRecipients`는 C6 4메서드 외 read이나 functional-design FE "발신 대상 선택" 화면에 근거한 UX 보조. 권위는 서버 `send()`의 canMessage(403).
- **결정**: "활성 신청" = enrollment `APPLIED`(U4 현행 enum 유일 비취소 리터럴). U4 상태 확장 시 재검토(memory 기록).
- **이탈(경미)**: FE 세션에 userId 부재(token+role만) → ThreadView는 스레드의 `partnerId`로 내/상대 메시지를 구분(senderId≠partnerId=내 메시지).
- **버그 수정**: ThreadView 초기 `load`가 `partnerId`에 의존해 재실행되며 로딩 플리커 발생 → partner 해석을 별도 effect로 분리(전문 로드는 `threadId`만 의존).
- **이월(Bolt 6+)**: 실시간 푸시/웹소켓(현재 폴링), 첨부·그룹 쪽지, U9 알림 조합.
