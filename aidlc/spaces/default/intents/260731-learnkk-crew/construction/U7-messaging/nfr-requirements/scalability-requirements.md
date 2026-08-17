# Scalability Requirements — U7 Messaging (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U7 Messaging(service). 출처: business-logic-model.md(폴링·스레드), business-rules.md, requirements.md(NFR2·NFR4·FR5.2 폴링). 파일럿·단일 인스턴스. 폴링 부하가 관심. -->

## 개요

requirements NFR2(동시 수십)·NFR4(단일 인스턴스). 수평 확장 범위 밖. 폴링 반복 쿼리가 부하 주 요인.

## 부하 전망

- unreadCount 폴링: 접속 사용자 수 × 폴링 빈도. 파일럿(동시 수십·30~60초 주기)이면 초당 수 회 수준 — 스레드 조인 + read_at 인덱스 집계로 여유(recipient 파생, 비정규화 컬럼 없음).
- 메시지·스레드: 완만 증가. thread_id 인덱스로 조회 선형.

## 확장 전략

- 단일 JVM·단일 DB로 충족. 폴링이 커지면(범위 밖) SSE/롱폴링 또는 캐시 재검토 — 채팅형 전환(OQ2) 시. 현 스레드형·폴링 유지.
- 데이터: 메시지 지속 증가 — 오래된 스레드 아카이빙 [open].

## Assumptions & Open Questions

- **[assumption]** 폴링 주기로 부하 제어, 인덱스 집계.
- **[open]** SSE/롱폴링 전환(채팅형 시), 메시지 아카이빙(=조회 계층 분리, 파기 아님 — NFR5 영속 보존 유지) — 범위 밖.
