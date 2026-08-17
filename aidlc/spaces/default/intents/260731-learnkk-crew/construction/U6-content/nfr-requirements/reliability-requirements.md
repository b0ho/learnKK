# Reliability Requirements — U6 Content (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U6 Content(service). 출처: business-logic-model.md(업로드·다운로드), business-rules.md(BR-U6-2), requirements.md(NFR4·NFR5·FR4.4). 첨부 무결성 중심. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖)·NFR5(영속 보존). U6 신뢰성은 **첨부 무결성**과 업로드 원자성.

## 가용성

- SLA/SLO 없음(파일럿). HA·복구 후속(NFR4).

## 데이터 무결성 (NFR5)

- 첨부 업로드는 단일 트랜잭션(post_attachment insert). 부분 저장 방지.
- BLOB(bytea)은 DB 트랜잭션 내 저장 — 파일시스템 별도 저장 대비 원자성 이점(메타-데이터 불일치 없음, ADR-004).
- 게시글/첨부/공지 영속 보존(NFR5).
- sizeBytes·contentType 메타가 실제 data와 일치 보장(업로드 시 산출).

## 장애 처리

- 업로드 중 크기/형식 위반 → 400, 저장 안 함(부분 저장 없음).
- 다운로드 중 오류 → 5xx 명시적(silent 실패 금지). 손상 BLOB은 파일럿엔 미고려(트랜잭션 저장이라 희박).
- DB 오류 시 트랜잭션 롤백.

## graceful degradation

- 게시글 목록에서 특정 첨부 메타 오류 시 해당 항목만 표시 처리, 목록 전체 실패 회피.

## Assumptions & Open Questions

- **[assumption]** 업로드 단일 트랜잭션, 메타-데이터 정합.
- **[open]** 첨부 백업·복구(bytea는 DB 백업에 포함되나 크기 큼) — 범위 밖.
- 백업·HA는 범위 밖(NFR4).
