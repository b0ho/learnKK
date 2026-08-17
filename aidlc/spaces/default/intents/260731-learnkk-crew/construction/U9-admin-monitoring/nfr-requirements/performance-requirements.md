# Performance Requirements — U9 Admin/Monitoring (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 출처: business-logic-model.md(승인 큐·모니터링 read 조합), business-rules.md(BR-U9-2/3), requirements.md(NFR2·NFR3). U1 baseline 상속. read 조합 성능이 관심. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U9는 **다중 Unit read 조합**(승인 큐·모니터링)이 성능 관심.

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| getApprovalQueues | < 2초 | U3 상태별 목록 + U5 수료후보 조합(관리자 저빈도) |
| getMonitoring | < 2초 | U3(상태·정원) × U4(신청 수) × U5(출석·수료) read 조합 |

## 핵심 성능 고려

- **read 조합 비용:** 큐/모니터링은 여러 Unit Service read를 합친다. 파일럿 규모(모임 수십)라 조합 비용 낮음. N+1 회피: 모임 목록을 배치 조회 후 U4/U5 집계를 배치로 결합([assumption]) — 모임당 개별 왕복 지양.
- **관리자 저빈도:** 대시보드는 관리자 소수·저빈도 조회라 스파이크 없음.
- 상태별 목록은 U3 `status` 인덱스 활용(U3 소유). U9 자체 인덱스 없음(소유 데이터 없음).

## Assumptions & Open Questions

- **[assumption]** 배치 조합(N+1 회피), 관리자 저빈도.
- **[open]** 모니터링 대량 모임 시 페이지네이션·집계 사전계산 — 파일럿 범위 밖(FR9.2 TBD).
- 엄격 부하 테스트는 performance-validation(범위 밖).
