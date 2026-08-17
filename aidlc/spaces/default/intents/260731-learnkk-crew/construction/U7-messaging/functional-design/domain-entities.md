# Domain Entities — U7 Messaging (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U7 Messaging(kind=service). 스토리: US-5.1(unit-of-work-story-map.md). 출처: unit-of-work.md(U7=C6 쪽지 스레드·미확인 뱃지·권한 경계·멘토-멘티 관계 U3/U4 read), requirements.md(FR5.1~5.3·인앱 폴링·OQ2 채팅형 미확정), components.md(C6·소유 데이터 message_thread/message), component-methods.md(MessageService send/listThreads/getThread/unreadCount), services.md, U1(ErrorPayload·Pagination·Principal). 권한은 U3/U4 read. Entity API 비노출(NFR8). -->

## 개요

U7은 C6(쪽지) 도메인 엔티티 `message_thread`·`message`를 소유한다. 전달은 인앱 확인(폴링, FR5.2) — 푸시/이메일 없음. 권한 경계(멘토↔자기 모임 멘티, 관리자↔전원)는 U3(모임 소유)·U4(신청) read로 판정. 채팅형 전환(OQ2)은 미채택 — 스레드형 기본.

## 엔티티

### MessageThread (쪽지 스레드)

두 사용자 간 대화 스레드. US-5.1.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `participantA` / `participantB` | BIGINT (FK→user) | NOT NULL | 두 참여자(정규화: min/max로 순서 고정) |
| `createdAt`/`lastMessageAt` | timestamptz | | 정렬용 |

- 두 사용자쌍당 스레드 1개 [assumption]: `unique(participantA, participantB)`(min/max 정규화로 방향 무관). 관리자↔멘토/멘티, 멘토↔멘티 조합.

### Message (쪽지)

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `threadId` | BIGINT (FK→message_thread) | NOT NULL | |
| `senderId` | BIGINT (FK→user) | NOT NULL | |
| `body` | text | NOT NULL | 쪽지 내용 |
| `readAt` | timestamptz | nullable | 수신자 확인 시각(미확인 뱃지 판정) |
| `createdAt` | timestamptz | NOT NULL | |

- **미확인 판정:** 수신자 기준 `readAt IS NULL` 메시지 수 = 미확인 뱃지(FR5.2 폴링). readAt은 수신자가 스레드 열람 시 갱신.

## 관계·통합 지점 (읽기 교차참조)

- `senderId`/participants → user(U2, Principal).
- **권한 경계(FR5.1):** 발신 허용 판정(`canMessage(sender, recipient)`):
  - 관리자(sender.role==ADMIN): 전원에게 발신 가능.
  - 멘토(sender.role==MENTOR): **자기 모임에 신청한 멘티**에게만. 판정 = U3(sender 소유 모임들) + U4(그 모임의 APPLIED 멘티에 recipient 포함?) read.
  - 멘티(sender.role==MENTEE): 자기가 신청한 모임의 멘토 또는 관리자에게 [assumption].
  - **의존 방향:** U7 → U3, U7 → U4 read. **U3·U4는 U7에 의존하지 않으므로 비순환**(U7 depends_on=[U1,U2,U3]에 U4 read를 확장 — unit-of-work.md U7 노트 "멘토-멘티 관계 권한은 U3/U4 read"가 예고). 각 Service 인터페이스 경유(모듈 소유 준수).

## 생명주기

- Thread: 첫 메시지 시 생성(없으면). Message: 발신 → (수신자 열람 시 readAt).

## Assumptions & Open Questions

- **[decided/OQ2]** 스레드형 유지(채팅형 전환 미채택 — FR5.3 설계 여지는 후속). 인앱 폴링(FR5.2), 푸시 없음.
- **[assumption]** 사용자쌍당 스레드 1개, 멘티 발신 대상(자기 모임 멘토·관리자), 관계 판정 read 포트.
- **[open]** `canMessage` 판정용 U3/U4 read 포트 시그니처(U3/U4 계약 정합). U7→U4 read edge는 비순환.
- 미확인 뱃지 폴링 주기는 FE 설정([assumption]).
