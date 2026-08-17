# Scalability Requirements — U5 Session/Attendance (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U5 Session/Attendance(service). 출처: business-logic-model.md(출석·수료), business-rules.md, requirements.md(NFR2·NFR4), services.md(스케줄러리스). 파일럿·단일 인스턴스. 출석 스파이크가 관심. -->

## 개요

requirements NFR2(동시 수십)·NFR4(단일 인스턴스). 수평 확장 범위 밖.

## 부하·동시성

- **출석 스파이크:** 세션 예정 시각에 멘티 다수 동시 checkIn. `unique(session,mentee)` 멱등 upsert로 경합 안전(중복 무해). 파일럿(모임당 수십)이라 단일 인스턴스 수용.
- **수료 집계:** computeCompletion은 참여자 수에 비례. 파일럿 규모라 온디맨드 충분.

## 확장 전략

- 단일 JVM·단일 DB. 수평 확장 범위 밖(NFR4). 스케줄러리스라 배치 인프라 불요.
- 데이터: session/attendance 증가는 완만(모임당 세션 수·멘티 수 제한적).

## Assumptions & Open Questions

- **[assumption]** 멱등 upsert로 출석 경합 안전, 온디맨드 집계.
- **[open]** 대규모 시 수료 집계 캐시/사전계산 — 범위 밖.
