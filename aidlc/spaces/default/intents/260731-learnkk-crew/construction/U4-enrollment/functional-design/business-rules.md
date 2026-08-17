# Business Rules — U4 Enrollment (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U4 Enrollment(service). 스토리 US-3.2/3.3/3.5(unit-of-work-story-map.md). 출처: unit-of-work.md(U4 선착순·정원/중복·취소), requirements.md(FR3.2 신청·FR3.3 선착순·FR3.5 취소/②후 이탈불가), components.md(C3), component-methods.md(EnrollmentService), services.md(정원·상태 U3 read), U1 business-rules(CC-1·인가). 정원·상태는 U3 read(ADR-007 R-1). -->

## 개요

U4는 신청 접수의 무결성(선착순 정원·중복 방지·취소 경계)을 소유한다. 모임 상태·정원은 U3 read(ADR-007 R-1, U4→U3). U1 CC-1 에러 매핑 상속.

## BR-U4-1. 선착순 정원 (US-3.2, FR3.3) — 동시성 핵심

- **불변식:** 활성(APPLIED) 신청 수 ≤ 모임 capacity(U3 read). **초과 신청 없음(overbooking 금지)**.
- **신청 가능 조건:** 대상 모임 status == RECRUITING(U3 read). 그 외(개설신청/시작대기/진행중/종료) → 409 `ENROLLMENT_NOT_OPEN`.
- **동시성(잔여 1석 경합) 보장:** 다음 중 하나로 원자적 판정(구현 [open], 무결성은 필수). 어느 방식도 U3 소유 `meeting` 행을 직접 잠그지 않는다(모듈 소유 규칙 — 타 모듈 테이블 직접 접근 금지, components.md):
  - (a) 신청 트랜잭션에서 모임 단위 직렬화 = 어드바이저리 락 `pg_advisory_xact_lock(meetingId)`(테이블 락 아님) → 활성 신청 수 count → capacity 미만이면 insert. (U4 소유 카운터/집계 행을 두고 `FOR UPDATE`하는 변형도 가능하나 meeting 행은 잠그지 않음.)
  - (b) SERIALIZABLE 격리로 count-then-insert, 직렬화 실패 시 재시도/거부.
- 정원 마감 시 → 409 `ENROLLMENT_FULL`(또는 마감 안내). 경합 패자는 마감으로 귀결.

## BR-U4-2. 중복 신청 방지 (US-3.2)

- 한 멘티가 한 모임에 활성 신청 1건. DB `unique(meeting_id, mentee_id)` + 애플리케이션 선검증.
- 중복 시 409 `ENROLLMENT_DUPLICATE`. 동시 중복 신청 경합은 unique 위반→409.

## BR-U4-3. 신청 취소 (US-3.3, FR3.5)

- **허용 시점:** 모임 status ∈ {RECRUITING, READY_TO_START} (즉 ②시작 전). ②시작(IN_PROGRESS) 이후 취소 불가 → 409 `ENROLLMENT_CANCEL_FORBIDDEN`(FR3.5 이탈 없음).
- 취소 시 status=CANCELLED, cancelledAt 설정. 정원 카운트에서 제외(빈자리 복귀 — 다른 멘티 신청 가능).
- 본인만 취소(menteeId==Principal.userId) — 아니면 403.

## BR-U4-4. 신청자 목록·현황 (US-2.3 read / US-3.5)

- `listApplicants(mentorId, meetingId)`: 소유 멘토/관리자만(403 경계). U3 운영 허브 화면이 read.
- `listMyEnrollments(menteeId)`: 본인 신청 목록·상태·다음 액션. 세션 일정(U5)은 **FE 화면 조합**(백엔드 U4→U5 금지, 순환 회피).

## BR-U4-5. 인가

- 신청·취소: role=MENTEE + 본인(Principal). 관리자·멘토는 신청 주체 아님.
- 신청자 목록: 소유 멘토/관리자만.

## 에러 처리 (U1 CC-1 상속)

- 정원 마감/중복/취소불가/모집아님 409, 인가 403, 검증 400, 미존재 404. ErrorPayload·한국어.

## Assumptions & Open Questions

- **[assumption]** 취소 후 재신청 불가, 대기열 없음.
- **[open]** 정원 동시성 락 방식((a) 어드바이저리/FOR UPDATE vs (b) SERIALIZABLE) — 구현 확정. 무결성(overbooking 금지)은 필수.
- **[open]** U3 정원·상태 read 포트 시그니처(U3 functional-design 정합).
- READY_TO_START에서 취소 허용은 이 Unit 규칙(BR-U4-3, ②전).
