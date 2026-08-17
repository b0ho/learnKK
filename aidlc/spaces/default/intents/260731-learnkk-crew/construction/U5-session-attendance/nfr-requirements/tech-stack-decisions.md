# Tech Stack Decisions — U5 Session/Attendance (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U5 Session/Attendance(service). 출처: business-logic-model.md(시간창·수료 판정), business-rules.md(BR-U5-2/4), requirements.md(C1·FR6.2·FR7.1), services.md(스케줄러리스 ADR-005). U1 tech-stack 상속. U5는 시간 판정·수료 계산 기술 선택. -->

## 개요

U1 스택·계약 도구 상속. U5는 스케줄러리스 시간 판정·수료 계산의 구체 기술을 확정.

## U5 기술 선택

### TD-U5-1. 스케줄러리스 시간 판정 (ADR-005)

- **결정:** 백그라운드 스케줄러·배치(Quartz 등) 미도입. 출석 유효 시간창은 checkIn 요청 시점 서버 `now`와 `session.scheduledAt` 비교로 판정.
- **근거:** 팝업 트리거는 FE 클라이언트 타이머, 서버는 검증만 — 배치 인프라 불요·장애 표면 축소(services.md·ADR-005).
- **Reversibility:** 높음(요청 시점 판정이라 로직 국소).

### TD-U5-2. 수료 판정 — 정수 연산 온디맨드

- **결정:** a*100≥80*S 정수 비교(부동소수 회피). computeCompletion은 온디맨드 집계(사전계산·캐시 없음), 결과를 mentee_completion에 upsert.
- **근거:** 정확·재현 가능, 파일럿 규모라 온디맨드로 충분.

### TD-U5-3. 출석 멱등 — DB unique upsert

- **결정:** `unique(session_id, mentee_id)` + upsert(ON CONFLICT DO NOTHING). 중복 checkIn 무해.
- **근거:** 동시·재시도 출석 경합을 DB 수준에서 안전 처리.

### TD-U5-4. mentee_completion 소유 테이블

- **결정:** U5가 `mentee_completion(meetingId, menteeId, status, attendedCount, totalScheduled, approvedAt)` 소유. CompletionStatus는 U1 enum(varchar+CHECK).

## 범위 밖

- 스케줄러/배치 프레임워크, 알림 푸시(FR5.2 밖), 실시간 출석 대시보드. CI/CD·운영(C3).

## Assumptions & Open Questions

- **[decided]** 스케줄러리스, 정수 판정, 멱등 upsert, mentee_completion U5 소유.
- **[assumption]** 시간창 길이(120분), 온디맨드 집계(캐시 없음).
- **[open]** U3/U4 read 포트 시그니처(참여자·모임 상태), 세션 변경 통지 방식(A6).
