# Performance Requirements — U7 Messaging (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U7 Messaging(service). 출처: business-logic-model.md(발신·스레드·unreadCount 폴링), business-rules.md(BR-U7-2 미확인), requirements.md(NFR2·NFR3·FR5.2 폴링). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U7은 **미확인 뱃지 폴링**이 성능 관심(주기적 반복 쿼리).

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| unreadCount(폴링) | < 200ms | 스레드 조인 + read_at 인덱스 집계, 자주 호출 |
| 스레드 목록 | < 1초 | 참여자 인덱스 + 페이지네이션 |
| 스레드 조회 | < 1초 | thread_id 인덱스 |
| 발신 | < 500ms | 관계 판정 read + insert |

## 핵심 성능 고려

- **폴링 부하:** `unreadCount`는 FE가 주기 폴링 → 호출 빈도 높음. 미확인 집계는 functional-design 모델대로 **본인 참여 스레드(message_thread participantA/B) 조인 + message(readAt IS NULL AND senderId != user)** 로 산출한다(별도 recipient 비정규화 컬럼 없음). 인덱스는 `message(thread_id, sender_id, read_at)` + `message_thread(participantA/participantB)` 로 조인 집계를 지원. 파일럿 규모(스레드·메시지 수십~수백)라 조인 집계도 경량. 폴링 주기는 FE가 합리적으로(예: 30~60초, [assumption]) — 과도 폴링 회피.
- **관계 판정 read:** 발신 시 canMessage가 U3/U4 read — 경량 관계 조회(인덱스). 파일럿 규모라 부담 낮음.
- 스레드/메시지: thread_id·created_at 인덱스로 시간순 페이지.

## Assumptions & Open Questions

- **[assumption]** 폴링 주기 30~60초, `message(thread_id, sender_id, read_at)` + `message_thread` 참여자 인덱스(recipient 파생 — 별도 컬럼 없음).
- **[note]** 미확인 집계는 스레드 참여 파생(비정규화 recipient 컬럼 미도입) — functional-design W3/BR-U7-2와 정합.
- **[open]** 폴링 대신 롱폴링/SSE는 채팅형 전환(OQ2) 시 재검토 — 현 범위 밖.
- 엄격 부하 테스트는 performance-validation(범위 밖).
