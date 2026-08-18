# Reliability Requirements — U3 Meeting (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U3 Meeting(service). 출처: business-logic-model.md(전이·③완료 전제), business-rules.md(BR-U3-1/5), requirements.md(NFR4 단일 인스턴스·NFR5 보존). 상태 무결성 중심. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖). U3 신뢰성은 **상태머신 무결성**과 **전이 원자성**에 집중.

## 가용성

- SLA/SLO 없음(파일럿). HA·복구는 후속(NFR4).

## 데이터 무결성 (NFR5)

- 상태 전이는 단일 트랜잭션·조건부 UPDATE로 원자적 — 부분 전이·이중 전이 방지(BR-U3-1).
- 종료 상태 불변(REJECTED/CANCELLED/COMPLETED) — 재전이 불가로 이력 일관.
- ③완료 전제(전 세션 종료)는 U5 read로 검증 후 전이 — 시점 정합(TOCTOU 최소화: 완료 액션 트랜잭션 내 status 재확인).
- meeting/survey_template 영속 보존(NFR5).

## 장애 처리

- U5 read 실패(③완료 전제 확인 불가) 시 완료 액션 5xx/명시적 오류 — silent 진행 금지(construction guardrail). 관리자에 재시도 유도.
- DB 오류는 트랜잭션 롤백으로 상태 일관 유지.

## graceful degradation

- 운영 허브에서 U4/U8 조합 호출 일부 실패 시, 화면은 U3 모임 데이터는 렌더하고 실패 부분만 부분 오류 표시(FE 조합 이점) — 전체 화면 실패 회피.

## Assumptions & Open Questions

- **[assumption]** 완료 액션 트랜잭션 내 status 재확인(TOCTOU 방지).
- **[open]** ③완료 전제 read 실패 시 UX(재시도) 상세.
- 백업·복구·HA는 범위 밖(NFR4).
