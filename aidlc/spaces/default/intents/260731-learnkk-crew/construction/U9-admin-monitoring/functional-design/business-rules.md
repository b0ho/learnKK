# Business Rules — U9 Admin/Monitoring (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 스토리 US-9.1/9.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U9·조회 read 조합·ADR-007), requirements.md(FR9.1 대시보드·FR9.2 집계 TBD·US-9.3 Won't), components.md(C8), component-methods.md(AdminQueryService), services.md(관리자 조회 read 조합), U1 business-rules(CC-1·MeetingStatus/CompletionStatus·인가). -->

## 개요

U9는 관리자 대시보드용 조회 규칙을 소유한다(read 조합, 소유 데이터 없음). 관리자 전용. 승인 액션은 소유 Unit(U3/U5). U1 CC-1 상속.

## BR-U9-1. 인가 — 관리자 전용

- 모든 U9 조회(getApprovalQueues/getMonitoring)는 `Principal.role == ADMIN`만 → 아니면 403(U1 BR-U1-5). 멘토·멘티 접근 불가.

## BR-U9-2. 승인 큐 집계 (US-9.1, FR9.1)

- 5개 큐를 타 Unit read로 집계(domain-entities 표):
  - `creation` = U3 status=PENDING_APPROVAL.
  - `recruitConfirm` = U3 status=RECRUITING(모집기간 종료된 것 [assumption]).
  - `start` = U3 status=READY_TO_START.
  - `meetingComplete` = U3 status=IN_PROGRESS AND U5 전 세션 종료(allScheduledSessionsEnded).
  - `menteeComplete` = U5 CompletionStatus=COMPLETION_CANDIDATE.
- U9는 큐 **표시**만 — 실제 승인은 각 큐 항목에서 소유 Service(U3 approveCreation/confirmRecruitment/approveStart/completeMeeting, U5 approveMenteeCompletion) 호출.
- ③=관리자 직접(rev-mk), 모집확정=독립 액션(U3 OQ1 해소)과 정합.

## BR-U9-3. 운영 현황 모니터링 (US-9.2, FR9.1)

- 모임별 상태(U3)·신청 수/정원(U4)·출석율(U5 세션 기준)·수료 진행(U5 후보/확정 수) 조합 표시.
- 읽기 전용 — U9는 write 없음.

## BR-U9-4. 집계 지표 범위 밖 (US-9.3 Won't, FR9.2)

- 개설 대비 승인 수·모집 충족률·평균 출석율·수료율·만족도·멘토 재개설률 등 **집계 지표는 이번 설계 범위 밖**(TBD 이월). U9는 목록·상태·큐·현황 조회만.

## BR-U9-5. read 조합 무결성

- 모든 데이터는 소유 Unit Service read(직접 테이블 접근 금지). 조합 시점 각 Unit의 권위 데이터 반영.
- U9→U3/U4/U5 read는 DAG 정방향(U9 최상위) — 비순환. 어느 Unit도 U9 read 안 함.

## 에러 처리 (U1 CC-1 상속)

- 비관리자 접근 403, 소스 read 실패 5xx(silent 금지). ErrorPayload·한국어.

## Assumptions & Open Questions

- **[decided]** 관리자 전용, U9=조회만(액션은 U3/U5), 집계 지표 범위 밖(US-9.3).
- **[assumption]** recruitConfirm 큐 판정(모집기간 종료 기준), 큐 항목 최소 필드.
- **[open]** U3/U4/U5 read 포트 시그니처(큐·모니터링 조합) 정합.
