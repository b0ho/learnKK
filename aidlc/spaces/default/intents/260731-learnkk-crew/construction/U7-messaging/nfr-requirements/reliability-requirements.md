# Reliability Requirements — U7 Messaging (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U7 Messaging(service). 출처: business-logic-model.md(발신·미확인), business-rules.md(BR-U7-2), requirements.md(NFR4·NFR5). 메시지 무결성 중심. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖)·NFR5(영속 보존). U7 신뢰성은 **메시지 전달 무결성**과 미확인 상태 일관.

## 가용성

- SLA/SLO 없음(파일럿). 인앱 확인 방식이라 실시간 전달 보장 불요(FR5.2).

## 데이터 무결성 (NFR5)

- 발신은 단일 트랜잭션(스레드 생성/조회 + 메시지 insert). 부분 저장 없음.
- readAt 갱신은 멱등 — 조건부 갱신(`UPDATE ... WHERE read_at IS NULL`, first-writer-wins)으로 동시 getThread write 경합에도 안전.
- 메시지·스레드 영속 보존(NFR5).

## 장애 처리

- 관계 판정(U3/U4) read 실패 시 발신 5xx/명시적 오류 — silent 실패 금지. 재시도 유도.
- 폴링 실패는 다음 폴링에서 복구(무상태 조회, 재시도 안전).
- DB 오류 시 트랜잭션 롤백.

## graceful degradation

- unreadCount 폴링 일시 실패 → 뱃지 이전값 유지·다음 주기 갱신(전체 UI 영향 없음).

## Assumptions & Open Questions

- **[assumption]** 발신 단일 트랜잭션, readAt 멱등.
- **[open]** 메시지 백업·아카이빙 — 범위 밖.
- 백업·HA는 범위 밖(NFR4).
