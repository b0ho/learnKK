# Tech Stack Decisions — U7 Messaging (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U7 Messaging(service). 출처: business-logic-model.md(폴링·스레드·canMessage), business-rules.md(BR-U7-4 OQ2), requirements.md(C1·FR5.2 폴링·FR5.3 채팅형 미확정). U1 tech-stack 상속. U7은 메시징 전달 방식 기술 선택. -->

## 개요

U1 스택·계약 도구 상속. U7은 쪽지 전달·미확인 기술을 확정.

## U7 기술 선택

### TD-U7-1. 전달 방식 — 인앱 폴링 (OQ2: 스레드형 유지)

- **결정:** 스레드형 인앱 쪽지 + FE 폴링(`unreadCount`). WebSocket/SSE/푸시 미도입(FR5.2).
- **근거:** 요구가 인앱 확인·폴링(FR5.2), 파일럿 규모라 폴링 충분. 채팅형(실시간) 전환(OQ2/FR5.3)은 후속.
- **Reversibility:** 중간 — 채팅형 전환 시 SSE/WebSocket 추가.

### TD-U7-2. 스레드 모델 — 정규화 pair 스레드

- **결정:** `message_thread(participantA,participantB)` min/max 정규화 + `unique` — 사용자쌍당 스레드 1개. 메시지는 thread_id FK.
- **근거:** 방향 무관 대화 스레드, 중복 스레드 방지.

### TD-U7-3. 관계 판정 — U3/U4 Service read

- **결정:** canMessage는 U3(모임 소유)·U4(등록 관계) Service read 조합(테이블 직접 접근 아님). U7→U3/U4 비순환.
- **근거:** 모듈 소유 준수, 관계 데이터는 소유 Unit이 권위.

## 범위 밖

- WebSocket/SSE/푸시(채팅형 전환 시), 메시지 큐(오버킬). CI/CD·운영(C3).

## Assumptions & Open Questions

- **[decided/OQ2]** 스레드형·폴링(채팅형 미채택).
- **[assumption]** 폴링 주기, pair 정규화.
- **[open]** 관계 read 포트 시그니처(U3/U4 정합), 채팅형 전환 시 SSE.
