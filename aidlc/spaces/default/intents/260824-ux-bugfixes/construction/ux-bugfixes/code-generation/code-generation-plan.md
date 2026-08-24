# Code Generation Plan — learnKK ux-bugfixes

12개 버그픽스(FR-1~FR-12)를 백엔드→프론트→테스트 순으로 구현한다. 브라운필드 in-place 수정. Minimal test strategy(회귀 위주). 각 단계 완료 시 체크.

## PART A. 백엔드 (Spring Boot)

### Step 1: DB 마이그레이션 V9 — 세션 완료 플래그 (FR-8)
- [ ] `backend/.../db/migration/V9__session_completed.sql`: `ALTER TABLE meeting_session ADD COLUMN completed boolean NOT NULL DEFAULT false;`

### Step 2: 세션 엔티티/DTO — 완료 상태 (FR-8)
- [ ] `MeetingSession`에 `completed` 필드 + `markCompleted()` + getter, `isEnded(now)= completed || windowEnd<now`.
- [ ] `SessionResponse`에 `completed` 필드 추가 + `from()` 반영.

### Step 3: SessionService — 삭제/완료, 종료 판정 갱신 (FR-7, FR-8)
- [ ] `deleteSession(principal, sessionId)`: loadSession → requireOwningMentor → `sessionRepository.delete` (attendance는 FK CASCADE로 함께 삭제).
- [ ] `completeSession(principal, sessionId)`: requireOwningMentor → `markCompleted()` 저장.
- [ ] `allScheduledSessionsEnded`: `s -> s.isEnded(now)` (수동 완료 OR 시간창 경과)로 변경.

### Step 4: SessionController — DELETE/complete 엔드포인트 (FR-7, FR-8)
- [ ] `DELETE /api/sessions/{id}` → deleteSession (204).
- [ ] `POST /api/sessions/{id}/complete` → completeSession (200, SessionResponse).

### Step 5: MeetingApprovalService — 역전이(되돌리기) (FR-5)
- [ ] `revert(principal, meetingId)`: requireAdmin → 현재 상태에서 직전 상태 매핑(RECRUITING→PENDING_APPROVAL, READY_TO_START→RECRUITING, IN_PROGRESS→READY_TO_START, COMPLETED→IN_PROGRESS) → transitionStatus(from,to,null). 대상 아니면 409 MEETING_INVALID_TRANSITION.

### Step 6: MeetingApprovalController — revert 엔드포인트 + 관리자 상태별 목록 (FR-5, FR-2/FR-3)
- [ ] `POST /api/admin/meetings/{id}/revert` → revert.
- [ ] `GET /api/admin/meetings?status=<STATUS>` (ADMIN) → 상태별 모임 목록(PageResponse<MeetingSummary>). MeetingApprovalService.listByStatus 추가(requireAdmin + meetingRepository.findByStatus).

### Step 7: EnrollmentService — 취소 후 재신청 (FR-12)
- [ ] `apply()`: 기존 레코드 조회 → APPLIED면 409 DUPLICATE; CANCELLED면 정원 검사 후 `reactivate()`(APPLIED 복귀·appliedAt 갱신·cancelledAt null)·저장; 없으면 신규 insert(현행).
- [ ] `Enrollment.reactivate()` 추가.

### Step 8: 계약(openapi.yaml) 갱신 (FR-2,5,7,8)
- [ ] `MeetingSessionResponse`에 `completed` 추가. 신규 경로: `DELETE /api/sessions/{id}`, `POST /api/sessions/{id}/complete`, `POST /api/admin/meetings/{id}/revert`, `GET /api/admin/meetings`(status 쿼리). tags 필요 시 보강.

## PART B. 프론트엔드 (React)

### Step 9: SurveyBuilder 쉼표 입력 (FR-1)
- [ ] CHOICE 선택지 입력을 로컬 raw-text 상태를 갖는 `ChoiceOptionsInput` 서브컴포넌트로 분리(표시=원문, 부모엔 trim·filter된 배열 전달). 매 입력마다 배열로 되접지 않음.

### Step 10: API 클라이언트 확장 (FR-2,5,7,8)
- [ ] `adminApi.listByStatus(status)`, `adminApi.revert(id)`.
- [ ] `sessionsApi.deleteSession(id)`, `sessionsApi.completeSession(id)`.
- [ ] `types.ts` `MeetingSessionResponse.completed?: boolean`.

### Step 11: AdminApprovalPage 재작성 (FR-2, FR-3, FR-5, FR-6)
- [ ] id 조회 제거. 상태별(PENDING_APPROVAL/RECRUITING/READY_TO_START/IN_PROGRESS/COMPLETED) 영역 섹션으로 목록 표시(adminApi.listByStatus 병렬 호출).
- [ ] 각 카드에 상태별 액션 버튼 + 되돌리기(해당 상태만) + 확인 다이얼로그(공용 confirm) 후 실행. 반려/모집취소는 기존 사유 다이얼로그 유지. CompletionPanel 재사용.

### Step 12: AppShell 관리자 네비 (FR-4)
- [ ] useAuth role 기반: ADMIN이면 '내 러닝' 자리에 '관리'(→/admin/meetings, Shield 아이콘) 노출.

### Step 13: MyLearningPage — 세션 삭제/완료 + 자료실 버튼 + 문항 편집 진입 (FR-7,8,9,10)
- [ ] MentorSessions: 세션별 '완료 처리'·'삭제' 버튼(완료/시간경과 뱃지), sessionsApi.complete/delete 후 reload.
- [ ] 자료실 링크(멘티/멘토)를 `<Button variant=outline + 아이콘>`으로 강조(testid 유지).
- [ ] MentorHub 카드에 상태 PENDING/RECRUITING/READY_TO_START일 때 '사전설문 문항 관리' 진입점(→ 문항 편집).

### Step 14: 사전설문 문항 편집 페이지 (FR-10)
- [ ] `MeetingQuestionsEditPage`: 기존 문항 로드(getQuestions)→SurveyBuilder→putQuestions 저장. 라우트 `/meetings/:id/questions-edit` + PATHS + AppRouter(멘토 접근).

### Step 15: FeedbackViewPage 분리 (FR-11)
- [ ] '과정 피드백' 섹션과 '사전설문 응답' 섹션을 별도 헤더로 분리(각 empty 상태 메시지).

## PART C. 테스트 (Minimal)

### Step 16: 백엔드 회귀 테스트
- [ ] SessionService: delete(출석 포함 삭제)·complete·isEnded 반영 테스트.
- [ ] MeetingApprovalService: revert 4개 전이 + 불가 상태 409.
- [ ] EnrollmentService: 취소 후 재신청(재활성화), APPLIED 중복 409, 정원 마감 409.
- [ ] OpenApiContractTest: MeetingSessionResponse.completed 등 계약 갱신 반영.

### Step 17: 프론트 회귀 테스트
- [ ] SurveyBuilder: 쉼표 포함 입력이 유지되는지(선택지 파싱).
- [ ] 기존 AdminApprovalPage.test/MyLearningPage.test/FeedbackViewPage.test는 새 구조에 맞게 갱신(깨지는 셀렉터 수정).

### Step 18: 검증
- [ ] 백엔드 `compileJava/compileTestJava` + 관련 테스트 green, `OpenApiContractTest` green.
- [ ] 프론트 `tsc --noEmit` green.
- [ ] DB V9 적용 후 앱 부팅(Flyway) 확인.

## 추적 (step→FR)
S1~S4,S13→FR-7/8 · S5,S6,S11→FR-5/2/3 · S7→FR-12 · S8,S10→계약 · S9→FR-1 · S12→FR-4 · S13→FR-9 · S14→FR-10 · S15→FR-11
