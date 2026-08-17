# Scalability Requirements — U4 Enrollment (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U4 Enrollment(service). 출처: business-logic-model.md(선착순), business-rules.md(BR-U4-1), requirements.md(NFR2·NFR4). 파일럿·단일 인스턴스. -->

## 개요

requirements NFR2(동시 수십 명)·NFR4(단일 인스턴스). 수평 확장 범위 밖.

## 부하·동시성

- 신청 동시성: 모임 단위 직렬화(어드바이저리 락/SERIALIZABLE). 서로 다른 모임 신청은 병렬 — 모임 단위 격리로 전체 처리량 확보.
- 같은 모임 인기 폭주 시 순차 처리(정원 정확성 우선). 파일럿 규모(정원 수십)라 순차 지연 미미.

## 확장 전략

- 단일 JVM·단일 DB로 목표 충족. 수평 확장 시(범위 밖) 어드바이저리 락은 DB 수준이라 다중 인스턴스에서도 동작하나, SERIALIZABLE 재시도 비용 재검토 필요 — [open].
- 데이터: enrollment 완만 증가. 취소 행 보존(감사) — 아카이빙 [open].

## Assumptions & Open Questions

- **[assumption]** 목표 규모 파일럿. 모임 단위 락.
- **[open]** 다중 인스턴스 시 락 전략, 취소 행 아카이빙 — 범위 밖.
