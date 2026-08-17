# Reliability Requirements — U4 Enrollment (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U4 Enrollment(service). 출처: business-logic-model.md(정원·취소), business-rules.md(BR-U4-1/2), requirements.md(NFR4·NFR5). 정원 무결성 중심. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖). U4 신뢰성은 **정원 무결성(overbooking 금지)**과 신청 원자성.

## 가용성

- SLA/SLO 없음(파일럿). HA·복구 후속(NFR4).

## 데이터 무결성 (NFR5)

- **overbooking 절대 금지:** 동시성 락(BR-U4-1)으로 활성 신청 수 ≤ capacity 보장 — 신뢰성의 핵심 불변식.
- 신청 insert는 단일 트랜잭션(원자). unique(meeting,mentee)로 중복 방지.
- 취소는 상태 전이(APPLIED→CANCELLED) 원자적, 취소 행 보존(감사·이력).
- enrollment 영속 보존(NFR5).

## 장애 처리

- SERIALIZABLE 채택 시 직렬화 실패(serialization_failure)는 재시도 또는 사용자 재시도 유도(409/명시적) — silent 실패 금지.
- DB 오류 시 트랜잭션 롤백으로 정원 일관 유지(부분 신청 없음).

## graceful degradation

- 멘티 현황 화면에서 U5/U3 조합 호출 일부 실패 시 U4 신청 데이터는 렌더, 실패 부분만 표시(FE 조합).

## Assumptions & Open Questions

- **[assumption]** 신청/취소 단일 트랜잭션.
- **[open]** SERIALIZABLE 재시도 정책(횟수·백오프) — 구현.
- 백업·복구·HA는 범위 밖.
