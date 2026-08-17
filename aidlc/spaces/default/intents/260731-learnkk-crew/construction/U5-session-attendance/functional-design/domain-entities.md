# Domain Entities — U5 Session/Attendance (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U5 Session/Attendance(kind=service, L). 스토리: US-6.2/6.3/7.1/7.2/7.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U5=C4 세션 일정·팝업 출석·출석율·80% 수료 판정·④ 확정·스케줄러리스 ADR-005), requirements.md(FR6.1~6.3·FR7.1·A6 세션 변경 통지·rev-us 분모=전체 예정 세션), components.md(C4·소유 데이터 session/attendance), component-methods.md(SessionService/AttendanceService/CompletionService), services.md(세션→출석→수료), U1(CompletionStatus enum·ErrorPayload·Principal). 참여자=수료 대상은 U4 read. Entity API 비노출(NFR8). -->

## 개요

U5는 C4(세션·출석·수료 판정) 도메인 엔티티 `session`·`attendance`를 소유한다. 수료 상태(CompletionStatus)는 U1 소유(정의), 판정 로직은 U5. 스케줄러 없이(ADR-005) 요청 시점에 시간창을 판정한다. 참여자(수료 대상)는 U4 read.

## 엔티티

### Session (수업 세션)

US-6.2. 멘토가 주차별 날짜·시간 지정, 주차당 복수 가능, 변경 가능(FR6.1).

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | U3 모임 |
| `week` | int | NOT NULL | 주차 |
| `scheduledAt` | timestamptz | NOT NULL | 예정 일시(변경 가능) |
| `checkInWindowMinutes` | int | 기본 [assumption] 예:120 | 출석 유효 시간창 길이 |
| `createdAt`/`updatedAt` | timestamptz | | |

- **출석 유효 시간창(A6/스케줄러리스):** `[scheduledAt, scheduledAt + checkInWindowMinutes]` 사이 요청만 출석 유효. 창 밖 checkIn 거부. 스케줄러 없이 checkIn 요청 시점에 `now`와 비교(ADR-005).
- 주차당 복수 세션 허용(같은 week 다수 row). 일정 변경은 scheduledAt update(멘티 현황 반영, A6).

### Attendance (출석)

US-6.3. 세션별 멘티 self check-in.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `sessionId` | BIGINT (FK→session) | NOT NULL | |
| `menteeId` | BIGINT (FK→user) | NOT NULL | |
| `checkedInAt` | timestamptz | NOT NULL | 출석 시각 |
| `unique(sessionId, menteeId)` | | | 세션별 1회(멱등) |

- **멱등:** `unique(session_id, mentee_id)` — 세션당 1회. 재요청은 기존 출석 유지(중복 무해).

### (참조) MenteeCompletion — 파생/조회 뷰

- 멘티 수료는 별도 저장보다 **enrollment(U4)에 CompletionStatus를 두거나** U5가 계산·확정 상태를 관리 [open]. 파일럿 결정: **U5가 `mentee_completion(meetingId, menteeId, status, approvedAt)` 테이블 소유**(status=CompletionStatus, U1). computeCompletion이 후보 판정, approveMenteeCompletion(④)이 확정.

| mentee_completion | 타입 | 비고 |
|---|---|---|
| `meetingId`/`menteeId` | BIGINT | 복합키 |
| `status` | varchar(enum CompletionStatus) | NOT_COMPLETED/COMPLETION_CANDIDATE/COMPLETED(U1) |
| `attendedCount`/`totalScheduled` | int | 판정 근거 스냅샷 |
| `approvedAt` | timestamptz | ④ 확정 시각 |

## 관계·통합 지점 (읽기 교차참조)

- `meetingId` → meeting(U3): 모임 상태(IN_PROGRESS 이후 출석 활성) read. 소유 멘토(세션 관리 권한) read.
- **수료 대상=참여자:** computeCompletion은 대상 멘티 집합을 U4(APPLIED 신청자) read로 확정. U5 depends_on U4(DAG) — 정방향, 비순환.
- **read-out(U5가 제공):**
  - `allScheduledSessionsEnded(meetingId)` → U3 ③완료 전제(ADR-007 R-2, U3→U5 read).
  - `listSessions(meetingId)` → U4 멘티 현황 화면(FE 조합)·U9 모니터링 read.
- **④ 수료 확정 액션:** 관리자(U9 승인 큐 화면에서 호출), 판정·확정 로직은 U5.

## Assumptions & Open Questions

- **[decided]** mentee_completion을 U5 소유 테이블로. CompletionStatus는 U1 enum.
- **[assumption]** 출석 유효 시간창 길이(checkInWindowMinutes 기본 120분), 과거 세션 편집 제약(A6).
- **[open]** 세션 변경 시 통지 방식(현황 반영, A6) — 인앱 현황 갱신 기본. 시간창 정확 정의는 team 확정.
- 분모=전체 예정 세션(FR6.3 rev-us). 세션 추가/삭제 시 S 변동 → 재계산(business-rules).
