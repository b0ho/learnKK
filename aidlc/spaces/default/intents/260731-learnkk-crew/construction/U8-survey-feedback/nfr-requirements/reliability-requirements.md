# Reliability Requirements — U8 Survey/Feedback (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U8 Survey/Feedback(service). 출처: business-logic-model.md(응답·피드백 제출), business-rules.md(BR-U8-1), requirements.md(NFR4·NFR5). 응답 무결성 중심. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖)·NFR5(영속 보존). U8 신뢰성은 **응답·피드백 무결성**과 제출 원자성.

## 가용성

- SLA/SLO 없음(파일럿). HA·복구 후속(NFR4).

## 데이터 무결성 (NFR5)

- 응답 제출은 단일 트랜잭션(다건 upsert). `unique(question,mentee)`로 문항당 1응답(재제출 갱신). 부분 저장 방지.
- 피드백 insert 원자. 응답·피드백 영속 보존(NFR5).
- 사전설문 응답은 U3 문항 틀(questionId FK)과 정합 — 문항 삭제 시 참조 무결성 [open](②후 문항 편집 금지가 U3 BR-U3-7로 보호).

## 장애 처리

- U3(문항·상태)·U4(참여자) read 실패 시 제출 5xx·명시적 오류(silent 금지).
- DB 오류 시 트랜잭션 롤백.

## graceful degradation

- 피드백 열람 조회 일부 실패 시 부분 표시.

## Assumptions & Open Questions

- **[assumption]** 응답 제출 단일 트랜잭션, 재제출 upsert.
- **[open]** 문항 삭제 시 응답 참조 무결성(U3 ②후 편집 금지로 보호), 백업 — 범위 밖.
