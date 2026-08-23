# Code Generation Plan — Bolt 5 Messaging (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 5 = U7 Messaging(쪽지 스레드·미확인 뱃지·권한 경계). Brownfield: Bolt 1~3 코드에 신규 messaging 모듈 추가 + 시임 배선. 상속 설계: 260731-learnkk-crew intent U7 functional-design(business-logic-model/business-rules/domain-entities)·nfr-requirements(security). 규칙: team.md(monorepo·3계층·계약우선·test-alongside·80% floor)·project.md(스택 lock·camelCase/snake_case·전역 에러·한국어 메시지). -->

## 목표 (Definition of Done — bolt-plan Bolt 5)

> B5 U7 DoD: 쪽지 스레드·미확인 뱃지·권한 경계. **가설:** 권한 경계(멘토=자기 모임 멘티, 관리자=전원)가 403으로 강제된다.

- **불변식(권한 경계): `canMessage(sender, recipient)`가 서버에서 강제** — 허용되지 않은 상대에게 발신/열람 시 403 MESSAGING_FORBIDDEN.
  - ADMIN → 전원 발신 가능.
  - MENTOR → 자기 모임에 활성(APPLIED) 신청된 멘티 + 관리자.
  - MENTEE → 자기가 신청한 모임의 멘토 + 관리자.
- 자기 자신에게 발신 → 400. 빈 본문 → 400. 존재하지 않는 상대 → 404.
- 스레드는 (참여자 쌍)당 1개(정규화 min/max). 열람 시 수신 메시지 확인 처리(멱등: `read_at IS NULL`만 UPDATE).
- 미확인 뱃지: 내가 수신자이고 `read_at IS NULL`인 메시지 총합(폴링용 경량 count).
- test-alongside, BE/FE 각 80% line floor + 권한 경계(403)·자기발신(400)·멱등 확인 시나리오.

## 상속·통합 지점 (기존 코드)

- **U7→U3 read**: 모임 소유(mentorId) 판정 — `MeetingService.getMeeting(id).mentorId()`(기존). 멘토의 모임 목록 id — 신규 read `MeetingService.meetingIdsOwnedBy(mentorId)`.
- **U7→U4 read**: 활성 신청 관계 — 신규 read `EnrollmentService.isActivelyEnrolledInAnyOf(meetingIds, menteeId)`, `EnrollmentService.activeMeetingIdsForMentee(menteeId)`, `EnrollmentService.activeMenteeIdsForMeetings(meetingIds)`. U3/U4 테이블 직접 접근 금지(Service 경유, ADR-007).
- **U7→U2 read**: 상대 닉네임/역할/존재 — `UserRepository`(findById, findByRole). 상대 존재 404, 역할로 canMessage 분기.
- **재사용**: kernel 에러 계층(Forbidden/NotFound/Validation), Principal/@AuthPrincipal, PageResponse/PageRequestFactory, SessionAuthInterceptor, JacksonConfig(camelCase).

## 범위 밖 (이월)

- 첨부·리치 텍스트(쪽지는 plain text). 실시간 푸시/웹소켓(폴링 뱃지만). 알림(U9 모니터링 조합). 대량 발신·그룹 쪽지.

---

## 실행 단계 (layer-by-layer)

### Step 1: DB 스키마 — V5 마이그레이션
- [ ] `backend/src/main/resources/db/migration/V5__messaging.sql`:
  - `message_thread`(id BIGINT identity PK, participant_a BIGINT NOT NULL FK→users ON DELETE CASCADE, participant_b BIGINT NOT NULL FK→users ON DELETE CASCADE, created_at timestamptz NOT NULL DEFAULT now(), last_message_at timestamptz, updated_at timestamptz). **CHECK (participant_a < participant_b)** + **CONSTRAINT uq_message_thread_participants UNIQUE (participant_a, participant_b)**(정규화된 쌍당 1스레드).
  - `message`(id BIGINT identity PK, thread_id BIGINT NOT NULL FK→message_thread ON DELETE CASCADE, sender_id BIGINT NOT NULL FK→users ON DELETE CASCADE, body text NOT NULL, read_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()).
  - 인덱스: `idx_message_thread_a`(participant_a), `idx_message_thread_b`(participant_b), `idx_message_thread`(thread_id, created_at), `idx_message_unread`(thread_id, sender_id, read_at).
- 추적: domain-entities MessageThread/Message, BR 스레드 유일성, V1~V4 규약

### Step 2: 도메인 — Entity + Repository
- [ ] `messaging/entity/MessageThread.java`(@Entity @Table("message_thread"): participantA/participantB by id, createdAt @CreationTimestamp, lastMessageAt, updatedAt @UpdateTimestamp; 정적 팩토리 `of(userX, userY)`가 min/max 정규화; `touch(OffsetDateTime)`로 lastMessageAt 갱신; 참여자 판정 `hasParticipant(userId)`·`partnerOf(userId)`).
- [ ] `messaging/entity/Message.java`(@Entity @Table("message"): threadId/senderId by id, body, readAt(nullable), createdAt @CreationTimestamp; `markRead(now)` — readAt이 null일 때만 세팅[멱등]).
- [ ] `messaging/repository/MessageThreadRepository.java`: `Optional<MessageThread> findByParticipantAAndParticipantB(Long a, Long b)`, `List<MessageThread> findByParticipantAOrParticipantBOrderByLastMessageAtDesc(Long a, Long b)`.
- [ ] `messaging/repository/MessageRepository.java`: `Page<Message> findByThreadIdOrderByCreatedAtAsc(Long threadId, Pageable)`, `@Modifying @Query` 벌크 확인 처리 `int markThreadReadForRecipient(threadId, recipientId, now)`(`UPDATE message SET read_at=:now WHERE thread_id=:t AND sender_id<>:r AND read_at IS NULL`), `int countByThreadIdInAndSenderIdNotAndReadAtIsNull(...)` 또는 뱃지용 `@Query` 합산 `countUnreadForUser(userId, threadIds)`.
- 추적: domain-entities, business-logic-model 멱등 확인

### Step 3: kernel — 에러 코드
- [ ] `kernel/error/ErrorCodes.java`에 `// --- Messaging domain ---`: `MESSAGING_FORBIDDEN`, `MESSAGING_SELF`, `MESSAGING_EMPTY_BODY`, `MESSAGING_RECIPIENT_NOT_FOUND`, `MESSAGING_THREAD_NOT_FOUND`.
- 추적: BR 권한/검증 매핑

### Step 4: 크로스모듈 read 메서드 (U3/U4)
- [ ] `MeetingService.meetingIdsOwnedBy(Long mentorId)` → `List<Long>`(read-only). MeetingRepository `@Query("select m.id from Meeting m where m.mentorId=:mentorId")`.
- [ ] `EnrollmentService`:
  - `boolean isActivelyEnrolledInAnyOf(Collection<Long> meetingIds, Long menteeId)` — repo `existsByMeetingIdInAndMenteeIdAndStatus(ids, menteeId, APPLIED)`(빈 컬렉션이면 false 단락).
  - `List<Long> activeMeetingIdsForMentee(Long menteeId)` — repo `@Query select e.meetingId ... status=APPLIED`.
  - `List<Long> activeMenteeIdsForMeetings(Collection<Long> meetingIds)` — repo `@Query select distinct e.menteeId ... meetingId in :ids and status=APPLIED`(빈 컬렉션 단락).
- [ ] `UserRepository.findByRole(Role)` 존재 확인, 없으면 추가(관리자 목록·전체 목록 read용).
- 추적: ADR-007 R-1(모듈 소유 경유), business-logic-model canMessage

### Step 5: MessageService (C6) — 권한 경계 + 스레드/발신/열람/뱃지
- [ ] 의존: MessageThreadRepository, MessageRepository, UserRepository, MeetingService, EnrollmentService.
- [ ] `private boolean canMessage(Principal sender, User recipient)`:
  - sender==recipient → false(별도 400 처리).
  - sender.isAdmin() → true. recipient.role==ADMIN → true.
  - sender.isMentor(): `isActivelyEnrolledInAnyOf(meetingIdsOwnedBy(sender), recipient.id)`.
  - sender.isMentee(): `activeMeetingIdsForMentee(sender.id)`의 각 모임 owner(mentorId)에 recipient.id 포함 여부(`meetingService.getMeeting(id).mentorId()` 조회 또는 owner set).
- [ ] `MessageResponse send(Principal sender, Long recipientId, String body)` `@Transactional`:
  - body trim 후 빈값 → 400 MESSAGING_EMPTY_BODY. recipientId==sender.userId → 400 MESSAGING_SELF.
  - recipient = UserRepository.findById → 없으면 404 MESSAGING_RECIPIENT_NOT_FOUND.
  - `!canMessage` → 403 MESSAGING_FORBIDDEN.
  - 스레드 find-or-create(정규화 쌍), `touch(now)`, message insert(sender, body). → MessageResponse.
- [ ] `List<ThreadSummaryResponse> listThreads(Principal)` `@Transactional(readOnly)`: 내 스레드(lastMessageAt desc), 각 스레드 partner(닉네임)·마지막 메시지·미확인 수 조합.
- [ ] `PageResponse<MessageResponse> getThread(Principal, threadId, Pageable)` `@Transactional`: 스레드 404, 참여자 아니면 403 MESSAGING_FORBIDDEN, 수신 메시지 확인 처리(markThreadReadForRecipient 멱등), 메시지 페이지(createdAt asc) 반환.
- [ ] `UnreadCountResponse unreadCount(Principal)` `@Transactional(readOnly)`: 내 스레드에서 상대 발신·미확인 합산 → {count}.
- [ ] `List<RecipientResponse> listRecipients(Principal)` `@Transactional(readOnly)`: 발신 가능 상대(FE 대상 선택 보조 — business-logic-model FE 화면). MENTOR=자기 모임 활성 멘티+관리자, MENTEE=신청 모임 멘토+관리자, ADMIN=본인 제외 전체. 서버 send()가 최종 권위(403).
- 추적: component-methods C6, BR 권한, business-logic-model, security-requirements(경계 서버 강제)

### Step 5b: DTO
- [ ] `messaging/dto/SendMessageRequest`(record: `@NotNull Long recipientId`, `@NotBlank String body`).
- [ ] `MessageResponse`(id, threadId, senderId, body, readAt, createdAt) `from(Message)`.
- [ ] `ThreadSummaryResponse`(threadId, partnerId, partnerNickname, lastMessageBody, lastMessageAt, unreadCount).
- [ ] `UnreadCountResponse`(int count).
- [ ] `RecipientResponse`(userId, nickname, role).

### Step 6: Controller + 인터셉터
- [ ] `messaging/web/MessageController.java`(@RestController):
  - `POST /api/messages`(send, 201) `@AuthPrincipal` + `@Valid @RequestBody SendMessageRequest`.
  - `GET /api/messages/threads`(listThreads, 200).
  - `GET /api/messages/threads/{id}`(getThread, 200, page/size 쿼리 → PageRequestFactory, allowedSort={"createdAt"}).
  - `GET /api/messages/unread-count`(unreadCount, 200).
  - `GET /api/messages/recipients`(listRecipients, 200).
- [ ] `SessionAuthInterceptor.isProtected` 확장: `path.startsWith("/api/messages")` → 전부 protected(인증 필요).
- 추적: 계약 #1, SessionAuthInterceptor

### Step 7: 백엔드 테스트 (Standard)
- [ ] `MessageServiceTest`(Mockito): send 정상(스레드 생성/재사용), 자기발신400, 빈본문400, 상대없음404, 권한없음403(멘토↔무관 멘티·멘티↔무관 멘토), 관리자 전원 허용; getThread 비참여자403·멱등 확인; unreadCount 합산.
- [ ] `MessageControllerTest`(@WebMvcTest, @MockBean AuthService + MessageService): 라우트 상태코드·401(무토큰)·403·400·201.
- [ ] `integration/MessageIntegrationTest`(Testcontainers): 멘토·멘티(자기 모임 활성 신청) 발신→수신 열람(확인 처리)→미확인 0, 무관 상대 발신 403, 관리자 발신 200 end-to-end. 스레드 유일성(양방향 발신 시 동일 스레드).
- 추적: team.md Testing Posture, DoD 가설(403 경계)

### Step 8: 계약 #1 — openapi.yaml
- [ ] version bump(`0.3.0-bolt3`→`0.4.0-bolt5`; Bolt 4 미구현이므로 5로), tag `messaging` 추가.
- [ ] paths: `/api/messages`(POST), `/api/messages/threads`(GET), `/api/messages/threads/{id}`(GET), `/api/messages/unread-count`(GET), `/api/messages/recipients`(GET). schemas: SendMessageRequest, MessageResponse, ThreadSummaryResponse, UnreadCountResponse, RecipientResponse. 403(MESSAGING_FORBIDDEN)·400·404 응답.
- [ ] `OpenApiContractTest` 신규 DTO 정합 확장.

### Step 9: Frontend API 계층
- [ ] `api/messages.ts`(`messagesApi`): `send(recipientId, body)`, `listThreads()`, `getThread(threadId, {page,size})`, `unreadCount()`, `listRecipients()`.
- [ ] `api/types.ts`: MessageResponse·ThreadSummaryResponse·UnreadCountResponse·RecipientResponse·SendMessageRequest. `api/index.ts` re-export.
- [ ] `api/errors.ts` ERROR_CODE_MESSAGES에 MESSAGING_* 한국어 매핑.

### Step 10: Frontend 쪽지함 화면 (features/messaging)
- [ ] `features/messaging/MessagesPage.tsx`: 스레드 목록(partner 닉네임·마지막 메시지·미확인 뱃지). 빈/로딩/에러 상태 `data-testid`.
- [ ] `features/messaging/ThreadView.tsx`(또는 라우트 `/messages/:threadId`): 메시지 목록(내/상대 구분, senderId 비교), 답장 입력·전송. 열람 시 확인 처리는 서버 GET에서 수행.
- [ ] `features/messaging/NewMessageDialog` 또는 대상 선택: `listRecipients()`로 허용 상대만 노출 → send. 403/400 한국어 매핑.
- 추적: business-logic-model FE 화면, CC 권한 경계

### Step 11: Frontend 라우팅 + 미확인 뱃지 (AppShell)
- [ ] `routes/paths.ts`: `messages='/messages'`, `messageThread:(id)=>'/messages/'+id`.
- [ ] `routes/AppRouter.tsx`: RequireAuth/AppShell 하위 `/messages`·`/messages/:threadId` 라우트.
- [ ] `routes/AppShell.tsx`: 하단 탭에 "쪽지"(MessageSquare 아이콘, testId `tab-messages`) 추가. `unreadCount()` 폴링(~30s) → 뱃지(count>0) 오버레이 `data-testid="unread-badge"`. 폴링 훅 `useUnreadCount` (features/messaging or hooks).
- 추적: 미확인 뱃지 DoD, business-logic-model

### Step 12: Frontend 테스트 (Vitest + RTL)
- [ ] MessagesPage 스레드 목록·미확인 뱃지, ThreadView 발신/열람·내상대 구분, 대상 선택 403 매핑, unread 뱃지 폴링, messages api 단위. `renderWithProviders`(auth), `vi.stubGlobal('fetch')`.

### Step 13: 문서
- [ ] README에 Bolt 5 범위(쪽지·권한 경계·미확인 뱃지) + 엔드포인트 반영.

---

## Assumptions

- "활성(active) 신청 관계" = enrollment status `APPLIED`(U4 현행 enum의 유일 비취소 리터럴). U4 상태 확장 시 재검토(memory 기록).
- 스레드는 1:1 쌍. 그룹 쪽지 없음. 쌍 정규화(min/max) + UNIQUE로 중복 스레드 방지.
- `listRecipients`는 FE 대상 선택 UX 보조 — 서버 send()의 canMessage(403)가 최종 권위. C6 4메서드 외 read이나 functional-design FE 화면에 근거.
- 미확인 뱃지 폴링 주기 = FE 설정(기본 30s). 실시간 푸시 없음.
- ci-pipeline·operation은 project.md Scope Override로 build-and-test 이후 SKIP.

## 테스트 전략 (Standard)

- 컴포넌트당 5~8 단위 + 권한 경계·스레드 유일성·멱등 확인 통합. 권한 경계(403)는 DoD 가설 핵심 → MessageServiceTest + MessageIntegrationTest 필수 커버. 이 환경 Testcontainers 미가용 시(Windows/Rancher JNA, Bolt 1~3 동일) 라이브 API E2E로 보완.
