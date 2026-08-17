# Business Rules — U5 Session/Attendance (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U5 Session/Attendance(service, L). 스토리 US-6.2/6.3/7.1/7.2/7.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U5·스케줄러리스 ADR-005·분모=전체 예정 세션·a*100≥80*S), requirements.md(FR6.1~6.3·FR7.1 80%·FR7.2 ④), components.md(C4), component-methods.md(SessionService/AttendanceService/CompletionService), services.md(세션→출석→수료 오케스트레이션·③ read U5), U1 business-rules(CC-1·CompletionStatus·인가). 수료 판정 수학이 핵심. -->

## 개요

U5는 세션 일정·출석·수료 판정을 소유한다. 스케줄러 없이(ADR-005) 요청 시점 시간창 판정. 출석율·80% 수료는 정수 연산으로 부동소수 오차를 피한다. U1 CC-1·CompletionStatus 상속.

## BR-U5-1. 세션 일정 (US-6.2, FR6.1)

- 세션 생성/변경은 소유 멘토(U3 read mentorId)만 → 아니면 403. 모임 status=IN_PROGRESS(②시작 이후)에서 활성 [assumption](또는 시작대기부터 일정 등록 허용 — team 확정).
- 주차당 복수 세션 허용. `scheduledAt` 변경 가능(멘티 현황 반영, A6). 과거(이미 종료·출석 발생) 세션 편집 제약 [assumption]: 출석 기록이 있는 세션의 scheduledAt 소급 변경 시 경고/제한.

## BR-U5-2. 팝업 출석 — 시간창 판정 (US-6.3, FR6.2, ADR-005)

- 출석은 멘티 self check-in. **유효 조건:** 요청 시점 `now ∈ [scheduledAt, scheduledAt + checkInWindowMinutes]`. 창 밖(이르거나 늦음) → **409** `ATTENDANCE_WINDOW_CLOSED`(타이밍/상태 충돌이므로 400 아닌 409, U1 CC-1).
- 스케줄러 없이 checkIn 요청 시 `now`와 세션 시간창 비교(ADR-005) — 백그라운드 잡 없음.
- 대상은 그 모임 참여자(APPLIED 멘티, U4 read). 비참여자 → 403.
- **멱등:** 세션당 1회(`unique(sessionId,menteeId)`). 재check-in 무해(기존 유지).
- 모임 status=IN_PROGRESS에서만 출석 가능(②시작 이후). 그 외 409.

## BR-U5-3. 출석율 산출 (US-7.4, FR6.3)

- 멘티 출석율 = (출석 세션 수 a) / (**전체 예정 세션 수 S**, rev-us). 분모는 모임의 전체 예정 세션(주차·복수 포함).
- S는 현재 등록된 세션 수 — 세션 추가/삭제 시 변동하므로 판정은 **판정 시점 S** 기준. 확정(④) 시 스냅샷 저장(attendedCount/totalScheduled).

## BR-U5-4. 80% 수료 자동 판정 (US-7.1, FR7.1)

- **판정식(정수 연산):** `a * 100 >= 80 * S` 이면 수료후보(COMPLETION_CANDIDATE), 아니면 미수료(NOT_COMPLETED). 부동소수 비교 회피(unit-of-work.md).
- S=0(세션 미등록) 경계 [assumption]: 판정 불가/미수료 처리(0으로 나눔 회피 — 식은 `a*100 >= 0` 항상 참이 되므로 S=0이면 후보 판정 보류).
- `computeCompletion(meetingId)`은 참여 멘티 각각 판정 → COMPLETION_CANDIDATE/NOT_COMPLETED 갱신. 자동(관리자 확정 전 단계).

## BR-U5-5. ④ 관리자 수료 확정 (US-7.2, FR7.1)

- `approveMenteeCompletion(admin, meetingId, menteeId)`: role=ADMIN 아니면 403.
- 대상이 COMPLETION_CANDIDATE 아니면(미충족) 409 `COMPLETION_NOT_ELIGIBLE`. 이미 COMPLETED면 409(중복).
- 확정 시 status=COMPLETED, approvedAt·스냅샷(a/S) 기록. 종료 상태(재전이 불가).
- ④는 U3의 ③(모임 완료)과 독립 — 모임 완료 없이도 개별 멘티 수료 확정 가능/또는 순서 [assumption]: 파일럿은 ③ 전후 무관하게 ④ 가능(관리자 판단).

## BR-U5-6. 인가

- 세션 관리: 소유 멘토. 출석: 참여 멘티 본인. 수료 판정 조회: 멘토(자기 모임)·관리자. ④ 확정: 관리자. 위반 403.

## 에러 처리 (U1 CC-1 상속)

- 시간창 밖 400/409, 미충족 확정 409, 인가 403, 미존재 404. ErrorPayload·한국어. 코드 `<DOMAIN>_<REASON>`.

## Assumptions & Open Questions

- **[assumption]** 시간창 길이(120분), 세션 활성 시점(IN_PROGRESS), S=0 경계, ④와 ③ 순서 무관, 과거 세션 편집 제약.
- **[open]** 세션 변경 통지(A6, 현황 반영), 시간창 정확 정의 — team 확정.
- **[decided]** 분모=전체 예정 세션(FR6.3), a*100≥80*S 정수 판정.
