# Reliability Requirements — U5 Session/Attendance (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U5 Session/Attendance(service). 출처: business-logic-model.md(출석·수료 판정·④), business-rules.md(BR-U5-4/5), requirements.md(NFR4·NFR5·FR7.1), services.md(스케줄러리스). 출석·수료 무결성 중심(성공지표). -->

## 개요

requirements NFR4(단일 인스턴스, HA·백업 범위 밖)·NFR5(영속 보존). U5 신뢰성은 **출석·수료 판정 무결성**(성공지표 80% 수료 직결).

## 가용성

- SLA/SLO 없음(파일럿). HA·복구 후속(NFR4).
- 스케줄러리스(ADR-005)라 배치 잡 실패·중복 실행 리스크 없음 — 신뢰성 단순화 이점.

## 데이터 무결성 (NFR5, 핵심)

- 출석: `unique(session,mentee)` 멱등 — 재시도·중복 요청에도 1회. 서버 시간 기준(클럭 정합: 단일 인스턴스라 시간 소스 단일).
- 수료 판정: 정수 연산(a*100≥80*S) — 재현 가능·부동소수 오차 없음. 세션 수(S) 변동 시 재판정으로 최신 반영.
- ④ 확정: 스냅샷(a/S·approvedAt) 보존 → 확정 후 세션 변경이 수료 결과 이력 훼손 못함(감사 일관).
- session/attendance/mentee_completion 영속 보존(NFR5).

## 장애 처리

- U3/U4 read 실패(참여자·모임 상태 확인 불가) 시 판정/출석 5xx·명시적 오류(silent 금지).
- 출석 upsert는 단일 트랜잭션. computeCompletion은 read-only 집계(부작용 없음, 재실행 안전).
- ④ 확정은 단일 트랜잭션(상태 전이 원자).

## graceful degradation

- 멘티 현황 화면 조합(U4+U5) 일부 실패 시 U5 출석·수료 부분은 렌더.

## Assumptions & Open Questions

- **[assumption]** 서버 단일 시간 소스(클럭 정합), 확정 스냅샷.
- **[open]** 다중 인스턴스 시 시간 소스·집계 정합 — 범위 밖(현 단일 인스턴스).
- 백업·HA는 범위 밖(NFR4).
