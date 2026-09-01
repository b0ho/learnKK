# Code Generation — Observation Diary (Bolt 8 Admin/Monitoring)

<!-- 오케스트레이터 유지. 손으로 편집하지 않음. -->

## Interpretations
- 2026-08-31T05:00:00Z — Bolt 8(Admin/Monitoring, U9) 대상. Bolt 2~7 패턴 동일: memory_path를 bolt 레벨(`construction/bolt8-admin/`)로 해석. 설계는 `260731-learnkk-crew` unit-of-work U9·ADR-007 상속. Brownfield — 신규 admin 모듈(read 조합) + 시임 배선.
- 2026-08-31T05:00:00Z — 범위 판독: bolt-plan Bolt 8 DoD 중 승인 큐(US-9.1)는 Bolt 2에서 `/api/admin/meetings`+AdminApprovalPage로 선구현 완료 확인 → 본 Bolt 실작업은 US-9.2(운영 현황 모니터링)로 확정. US-9.3은 Won't.

## Deviations
- 2026-08-31T09:00:00Z — [pre-build fix] `./gradlew build` 시 기존 테스트 11곳 컴파일 실패(FR-7 `mentorCompletionStatus` record 필드 추가 이후 미갱신 생성자 — 파킹된 WIP 트리 잔재). 본 Bolt 범위에 사전 정리로 편입(8개 테스트 파일).
- 2026-08-31T14:20:00Z — [pre-build fix] `EnrollmentIntegrationTest:84` 실패 — FR-12(취소 행 재사용 재신청) 도입 이후 미갱신 기대값(구 규칙 DUPLICATE). 현행 규칙에 맞게 ENROLLMENT_FULL로 수정 + APPLIED 중복 검증 블록 추가.
- 2026-08-31T13:00:00Z — [pre-build fix] FE `npm run build` 실패 — `AppShell.tsx` `TAB_ROOTS.includes(pathname)`이 `as const` PATHS 리터럴 유니온과 충돌. `readonly string[]` 명시로 해소.

## Tradeoffs
- 2026-08-31T05:30:00Z — [attendance-rate] '출석율(세션 기준)'을 총출석/(S×멘티수)로 정의 — 멘티별 a/S(BR-U5-3)의 모임 평균과 동치, 분모 0→0.0(AttendanceSummaryResponse 선례). 분모를 ended가 아닌 S로 두어 진행 중 세션 출석에 의한 1.0 초과 배제. endedSessionCount는 별도 지표로 노출.
- 2026-08-31T05:30:00Z — [cross-module] U9→U4는 `EnrollmentService.listActiveMenteeIds`(서비스 read), U9→U5는 repository read(AttendanceRepository.countByMeetingId 신규·MeetingSessionRepository·MenteeCompletionRepository), U9→U2는 userRepository→nickname(EnrollmentService.resolveNickname 선례). 타 모듈 테이블 직접 SQL 없음.
- 2026-08-31T05:30:00Z — [routing] 모니터링을 `/api/admin/monitoring/*`로 분리 — 승인 액션(`/api/admin/meetings`, U3/U5 소유)과 URL 경계로 ADR-007 소유권을 표현. FE도 read 전용 화면 + 승인 큐 상호 진입 버튼.
- 2026-08-31T05:30:00Z — [perf] 모임 행당 세션/멘티/수료 read(N+1)는 페이지 클램프(≤100)·조회 전용 특성으로 허용. 대량 시 배치 read 이월.

## Open questions
- 2026-08-31T05:30:00Z — [assumption] 멘토 닉네임 null 허용(FE `#mentorId` 폴백). 사번 노출은 개인정보 고려로 제외.
- 2026-08-31T05:30:00Z — [carry-over] US-9.3 집계 지표(FR9.2 TBD), 모니터링 정렬 UI(백엔드 sort 지원 완료), 배치 read 최적화.
