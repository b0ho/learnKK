# Domain Entities — U9 Admin/Monitoring (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U9 Admin/Monitoring(kind=service, read/조회 계층). 스토리: US-9.1/9.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U9=C8 승인 큐 집계·운영 현황 모니터링·소유 데이터 없음·타 모듈 read 조합 ADR-007), requirements.md(FR9.1 대시보드·FR9.2 집계 지표 TBD·US-9.3 Won't), components.md(C8·소유 데이터 없음), component-methods.md(AdminQueryService getApprovalQueues/getMonitoring), services.md(관리자 조회=read 조합), U1(Role·MeetingStatus·CompletionStatus·ErrorPayload). U9는 소유 엔티티가 없고 read 조합 DTO만 정의. -->

## 개요

U9는 **소유 데이터가 없는 read/조회 계층**이다(components.md C8, ADR-007). 자기 테이블·엔티티를 두지 않고, U3(모임 상태)·U4(신청)·U5(출석·수료) 등의 read를 조합해 관리자 대시보드용 뷰를 구성한다. 실제 승인 **액션**은 소유 Unit(U3 상태전이·U5 수료 판정)이며, U9는 **조회/집계**만 담당.

## 소유 데이터: 없음

- U9는 write 테이블을 소유하지 않는다. 모든 데이터는 타 Unit Service read로 조합(직접 테이블 접근 아님, 모듈 소유 준수).
- U9 depends_on U1,U2,U3,U4,U5,U8(DAG 최상위) — 모든 read가 정방향. **어느 Unit도 U9에 의존하지 않으므로** U9의 read 조합은 순환을 만들지 않는다.

## 조회 DTO (read 조합 뷰)

### ApprovalQueues (승인 큐 집계) — US-9.1

component-methods `getApprovalQueues → {creation[], recruitConfirm[], start[], meetingComplete[], menteeComplete[]}`. 각 큐는 타 Unit read 조합:

| 큐 | 의미 | 소스(read) | 판정 조건 |
|----|------|-----------|-----------|
| `creation[]` | ①개설 승인 대기 | U3 모임 | status=PENDING_APPROVAL |
| `recruitConfirm[]` | 모집 확정 대기 | U3 모임 | status=RECRUITING(모집기간 종료) |
| `start[]` | ②시작 승인 대기 | U3 모임 | status=READY_TO_START |
| `meetingComplete[]` | ③모임 완료 대기 | U3 모임 + U5 세션종료 read | status=IN_PROGRESS AND 전 세션 종료(U5 allScheduledSessionsEnded) |
| `menteeComplete[]` | ④멘티 수료 대기 | U5 수료 판정 | CompletionStatus=COMPLETION_CANDIDATE |

- 각 항목은 식별·표시용 최소 필드(모임 id/제목/멘토·멘티 id 등). 액션은 각 소유 Service 호출(U9는 큐 표시만).

### MeetingMonitorRow (운영 현황) — US-9.2

component-methods `getMonitoring → MeetingMonitorRow[]`. 모임별:

| 필드 | 소스(read) |
|------|-----------|
| meeting 기본·status | U3 |
| 정원(capacity) | U3(meeting 소유 필드) |
| 신청 수 | U4 |
| 출석율(세션 기준)·수료 진행 | U5 |

- 세션 기준 출석율(U5 getMyAttendance/computeCompletion 집계 조합). 수료 진행(후보/확정 수).

## 범위 밖 (US-9.3 Won't)

- 집계 지표(개설 대비 승인 수·모집 충족률·평균 출석율·수료율·만족도·멘토 재개설률)는 **이번 설계 범위 밖**(FR9.2 TBD 이월, US-9.3 Won't). U9는 목록·상태·큐·현황의 **조회**만.

## Assumptions & Open Questions

- **[decided]** U9는 소유 데이터 없는 read 조합 계층. 승인 액션은 U3/U5, U9는 조회.
- **[assumption]** 큐 항목 최소 필드, recruitConfirm 판정(모집기간 종료 기준).
- **[open]** U3/U4/U5 read 포트 시그니처(큐·모니터링 조합) 정합. 집계 지표(FR9.2)는 범위 밖.
