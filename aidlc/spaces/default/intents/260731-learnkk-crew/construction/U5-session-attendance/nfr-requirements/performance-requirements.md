# Performance Requirements — U5 Session/Attendance (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U5 Session/Attendance(service). 출처: business-logic-model.md(세션·출석·수료 판정), business-rules.md(BR-U5-2 시간창·BR-U5-4 판정), requirements.md(NFR2·NFR3), services.md(스케줄러리스). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U5는 **출석 체크(예정 시각 다수 동시)**·**수료 판정 집계**가 성능 관심.

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| checkIn(출석) | < 500ms | 시간창 비교 + upsert(단건) |
| getMyAttendance | < 500ms | session/attendance count(인덱스) |
| 세션 목록/생성 | < 500ms | meeting_id 인덱스 |
| computeCompletion(모임) | < 2초 | 참여자 × 출석 집계(파일럿 수십 멘티) |

## 핵심 성능 고려

- **출석 스파이크:** 예정 시각에 멘티 다수가 동시 팝업→checkIn. 각 checkIn은 시간창 비교 + `unique(session,mentee)` upsert(멱등)로 경량. 파일럿 규모(모임당 수십 멘티)라 스파이크 수용.
- **스케줄러리스(ADR-005):** 백그라운드 잡 없음 → 서버 유휴 부하 없음. 판정은 요청 시점만.
- **수료 집계:** computeCompletion은 참여자 × 세션 출석 count 집계. `attendance(session_id, mentee_id)`·`session(meeting_id)` 인덱스로 효율. 관리자 조회/판정 시점 온디맨드.

## Assumptions & Open Questions

- **[assumption]** attendance/session 인덱스, computeCompletion 온디맨드(캐시 없음).
- **[open]** 대규모 시 수료 집계 사전계산·캐시 — 파일럿 범위 밖.
- 엄격 부하 테스트는 performance-validation(범위 밖).
