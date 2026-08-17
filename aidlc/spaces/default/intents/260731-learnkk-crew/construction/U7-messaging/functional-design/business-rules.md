# Business Rules — U7 Messaging (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U7 Messaging(service). 스토리 US-5.1(unit-of-work-story-map.md). 출처: unit-of-work.md(U7·관계 권한 U3/U4 read), requirements.md(FR5.1 대상·FR5.2 인앱 폴링·FR5.3 채팅형 미확정), components.md(C6), component-methods.md(MessageService send/listThreads/getThread/unreadCount), U1 business-rules(CC-1·인가). -->

## 개요

U7은 쪽지 발신 권한 경계·미확인 판정·스레드 관리를 소유한다. 권한은 U3(모임 소유)·U4(신청) read로 관계 판정. U1 CC-1 상속.

## BR-U7-1. 발신 권한 경계 (US-5.1, FR5.1)

`send(senderId, recipientId, body)` 허용 규칙:

- **관리자(ADMIN):** 전원(멘토·멘티)에게 발신 가능.
- **멘토(MENTOR):** **자기 모임에 활성(취소되지 않은) 등록 관계인 멘티** 또는 관리자에게만. 무관계 멘티 발신 → 403 `MESSAGE_FORBIDDEN`. (단일 `APPLIED` 리터럴이 아니라 "활성 등록 관계" — ②시작 이후에도 관계 유지되어야 메시징 단절 없음; 정확한 상태 집합은 U4 계약 정합, S1.)
- **멘티(MENTEE):** 자기가 신청한 모임의 **멘토** 또는 관리자에게만 [assumption]. 그 외 403.
- 판정 `canMessage(sender, recipient)`은 U3(sender/recipient 모임 소유)·U4(신청 관계) read 조합. 위반 403.
- 자기 자신에게 발신 금지 → 400.

## BR-U7-2. 미확인(unread) 판정 (FR5.2)

- 수신자 기준 `readAt IS NULL AND senderId != 수신자`인 메시지 수 = 미확인 수.
- `unreadCount(userId)`: 전체 스레드의 미확인 합계(인앱 뱃지, 폴링). 푸시/이메일 없음(FR5.2).
- 스레드 열람(`getThread`) 시 해당 스레드의 수신 메시지 readAt 갱신(확인 처리).

## BR-U7-3. 스레드 관리

- 두 사용자쌍당 스레드 1개(정규화). 첫 발신 시 스레드 생성.
- `listThreads(userId)`: 본인 참여 스레드만(403 경계). 최신 메시지순 정렬.
- `getThread(userId, threadId)`: 본인 참여 스레드만(아니면 403). 메시지 페이지.

## BR-U7-4. 채팅형 전환 (OQ2)

- 파일럿은 **스레드형 유지**(component-methods C6 스레드 기본). 채팅형(실시간) 전환은 FR5.3 설계 여지 — 미채택, 후속. 현 설계는 폴링 기반 인앱 확인.

## BR-U7-5. 인가

- 발신: BR-U7-1 관계 규칙. 열람: 본인 참여 스레드만. 위반 403.

## 에러 처리 (U1 CC-1 상속)

- 발신 권한 위반 403 `MESSAGE_FORBIDDEN`, 자기 발신 400, 미존재 404. ErrorPayload·한국어.

## Assumptions & Open Questions

- **[decided/OQ2]** 스레드형 유지(채팅형 미채택). 인앱 폴링·푸시 없음.
- **[assumption]** 멘티 발신 대상(자기 모임 멘토·관리자), 사용자쌍당 스레드 1개.
- **[open]** `canMessage` 판정용 U3/U4 read 포트(관계 조회) 시그니처 — U3/U4 계약 정합. U7→U4 비순환.
