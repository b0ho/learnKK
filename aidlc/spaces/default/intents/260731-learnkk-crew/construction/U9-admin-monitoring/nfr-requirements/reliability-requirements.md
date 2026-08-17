# Reliability Requirements — U9 Admin/Monitoring (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 출처: business-logic-model.md(read 조합·graceful), business-rules.md(BR-U9-5), requirements.md(NFR4·NFR5). read-only라 무결성 리스크 최소. -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖). U9는 read-only 조회 계층이라 자체 데이터 무결성 리스크가 없고, **조합 read의 부분 실패 처리**가 신뢰성 관심.

## 가용성

- SLA/SLO 없음(파일럿). U9 자체 상태 없음 — 무상태 조회.

## 데이터 무결성 (NFR5)

- U9는 write 없음 → 무결성은 소유 Unit(U3/U4/U5) 책임. U9는 조합 시점 각 Unit의 권위 데이터를 반영(캐시 없음 → stale 없음).
- 집계 지표 미저장(범위 밖) → 집계 불일치 리스크 없음.

## 장애 처리 (부분 실패)

- 조합 read 중 일부 Unit read 실패 시: 해당 큐/모니터링 행만 오류 표시하고 나머지는 렌더(graceful degradation, business-logic-model). 전체 대시보드 실패 회피.
- read 실패는 5xx 명시적(silent 금지). 재조회 안전(무상태).

## graceful degradation

- 승인 큐 5개 중 일부 소스 실패 → 성공한 큐는 표시, 실패 큐는 오류 배지.
- 모니터링 특정 모임 집계 실패 → 해당 행만 부분 표시.

## Assumptions & Open Questions

- **[decided]** read-only(무결성은 소유 Unit), 캐시 없음(stale 없음), 부분 실패 graceful.
- **[open]** 부분 실패 표시 단위(큐/행) 상세는 code-generation 전 확정. 기본값: 전체 소스 실패=5xx / 부분 실패=200+행·큐별 오류 표시.
- 백업·HA는 범위 밖(NFR4).
