# Domain Entities — U4 Enrollment (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U4 Enrollment(kind=service). 스토리: US-3.2/3.3/3.5(unit-of-work-story-map.md). 출처: unit-of-work.md(U4=C3 선착순·정원/중복 제어·취소·현황), requirements.md(FR3.2/3.3/3.5·FR3.3 선착순), components.md(C3·소유 데이터 enrollment), component-methods.md(EnrollmentService apply/cancel/listApplicants/listMyEnrollments), services.md(모임 상태·정원 U3 read), U1(ErrorPayload·Principal·Pagination). 모임 상태·정원은 U3 read(ADR-007 R-1). Entity API 비노출(NFR8). -->

## 개요

U4는 C3(신청·모집) 도메인의 `enrollment` 엔티티를 소유한다. 모임 정원·상태는 U3 소유(read, ADR-007 R-1). 멘티 수료 대상 확정의 근거(참여자)를 U5가 read.

## 엔티티

### Enrollment (신청)

US-3.2 신청, US-3.3 취소.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | U1 baseline |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | U3 모임 |
| `menteeId` | BIGINT (FK→user) | NOT NULL | role=MENTEE |
| `status` | varchar(enum) | NOT NULL | APPLIED(신청됨)/CANCELLED(취소됨) |
| `appliedAt` | timestamptz | NOT NULL | 선착순 순서 근거 |
| `cancelledAt` | timestamptz | nullable | 취소 시각 |
| `createdAt`/`updatedAt` | timestamptz | | baseline |

- **중복 방지:** `unique(meeting_id, mentee_id)` — 한 멘티가 한 모임에 유효 신청 1건. (취소 후 재신청 정책은 [assumption] — 파일럿은 재신청 불가로 unique 유지, 재신청 허용 시 부분 unique 또는 status 포함 재설계.)
- **선착순 근거:** `appliedAt`(또는 id 순서)로 접수 순서 판정. 정원 판정은 활성(APPLIED) 신청 수 대비 모임 capacity(U3 read).

### EnrollmentStatus (신청 상태, U4 로컬 enum)

- `APPLIED`(신청됨), `CANCELLED`(취소됨). 모임 상태(MeetingStatus, U1/U3)와 별개 — 신청 자체의 상태.
- 멘티 수료 상태(CompletionStatus)는 U5 소유 — 여기 아님.

## 관계·통합 지점 (읽기 교차참조)

- `meetingId` → meeting(U3). **모임 상태·정원(capacity) read는 U4→U3**(ADR-007 R-1, DAG 방향 U4 depends_on U3와 일치 — 순환 아님). 신청 가능 여부(status=RECRUITING)·정원 판정에 사용.
- `menteeId` → user(U2, Principal).
- **US-3.5 멘티 현황(FE 화면 조합):** 멘티 현황 화면이 U4 `listMyEnrollments` + **U5 세션 일정(`listSessions`)**을 FE 단일 API client로 조합한다. **백엔드 U4→U5 read는 하지 않는다**(U5 depends_on에 U4 존재 → U4→U5 백엔드 read는 U4↔U5 순환. 화면 레벨 조합으로 회피).
- 신청자 목록(listApplicants)은 U4 소유 데이터 — U3 운영 허브 화면이 이를 read 조합(U3→U4 방향, ADR-007 R-1).

## 생명주기 (Enrollment)

```
apply → APPLIED
APPLIED ─(②시작 전, cancel)→ CANCELLED
APPLIED ─(②시작 후)→ (취소 불가, 이탈 없음 FR3.5; 수료는 출석율로 U5)
```

## Assumptions & Open Questions

- **[assumption]** 취소 후 재신청 불가(unique 유지). 재신청 허용 시 재설계.
- **[open]** 정원 동시성 락 방식(business-rules BR-U4-1) — SERIALIZABLE vs 어드바이저리 락 vs 조건부.
- **[open]** U3 정원·상태 read 포트, U5 세션 일정 read 시그니처는 U3/U5 계약 정합.
- 대기열(waitlist)은 범위 밖(선착순 마감이면 거부).
