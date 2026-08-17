# Scalability Requirements — U9 Admin/Monitoring (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 출처: business-logic-model.md(read 조합), business-rules.md, requirements.md(NFR2·NFR4·FR9.2 TBD). 파일럿·단일 인스턴스. read 조합 확장이 관심. -->

## 개요

requirements NFR2(모임 수십)·NFR4(단일 인스턴스). 수평 확장 범위 밖. read 조합 계층이라 자체 데이터 증가 없음.

## 부하 전망

- 조회는 관리자 소수·저빈도. read 조합은 모임 수에 비례 — 파일럿(수십)이면 경량.
- U9 자체 저장 없음 → 데이터 증가 요인 아님. 부하는 소스 Unit(U3/U4/U5) 조회로 위임.

## 확장 전략

- 단일 JVM·단일 DB로 충족. 모임 대량화 시(범위 밖) 모니터링 페이지네이션·집계 사전계산(FR9.2 TBD) 도입 여지 — 현 범위 밖.
- read 조합이 N+1이 되지 않도록 배치 결합(performance).

## Assumptions & Open Questions

- **[assumption]** 관리자 저빈도·소수, 배치 조합.
- **[open]** 대량 모임 시 모니터링 페이지네이션·집계 사전계산(FR9.2) — 범위 밖.
