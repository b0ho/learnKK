# Component Methods — learnKK (런크크)

<!-- application-design 산출물. 각 모듈 Service 공개 인터페이스의 메서드 시그니처(고수준). 상세 비즈니스 규칙은 functional-design 소관. 타입은 DTO/도메인 타입(C0) 기준(Entity 비노출, team-practices). 에러는 전역 스키마 {code,message,details}+상태코드(stories CC-1). 출처: components.md, stories, requirements. -->

## 표기

- 시그니처는 언어 중립 의사형(입력→출력). 예외는 CC-1 규약(400/401/403/404/409) 코드로 표기.
- DTO는 `XxxRequest`/`XxxResponse`, 도메인 enum은 C0 shared kernel.

## C1. Auth/User

### AuthService
- `signup(SignupRequest{nickname, password, employeeNo}) -> UserResponse` — 사번·닉네임 유일성 검증, bcrypt 해시 저장. 중복 시 409. [US-1.1/1.3]
- `login(LoginRequest{nickname, password}) -> SessionResponse{token, role}` — 실패 시 401(계정 존재 비특정). [US-1.2]
- `validateSession(token) -> Principal{userId, role}` — 무효/만료 401. [US-1.2]
- `logout(token) -> void` — 세션 무효화. [US-1.2]

### UserService
- `getProfile(userId) -> ProfileResponse{nickname, employeeNo, tags[], intro}` [US-1.4]
- `updateProfile(userId, ProfileUpdateRequest{tags[], intro}) -> ProfileResponse` — 검증 실패 400. [US-1.4]

## C2. Meeting

### MeetingService
- `createMeeting(mentorId, MeetingCreateRequest{title, topic, weeks, recruitPeriod, capacity, format, initialContent}) -> MeetingResponse` — 상태=개설신청. [US-2.1a]
- `getMeeting(meetingId) -> MeetingResponse` / `listRecruiting(filter) -> MeetingSummary[]` — 목록은 모집중 노출. [US-3.1]
- `listMyMeetings(mentorId) -> MeetingResponse[]` — 멘토 운영 허브. [US-2.3]

### SurveyTemplateService
- `upsertQuestions(mentorId, meetingId, SurveyQuestion[]) -> void` — 개설/수정 중. [US-2.1b]
- `getQuestions(meetingId) -> SurveyQuestion[]`

### MeetingApprovalService (관리자 액션)
- `approveCreation(adminId, meetingId) -> void` — ①: 개설신청→모집중. 잘못된 상태 409. [US-2.2]
- `rejectCreation(adminId, meetingId, reason) -> void` — →반려(종료). [US-2.2]
- `confirmRecruitment(adminId, meetingId, proceed:boolean) -> void` — 모집중→시작대기(또는 미달 시 proceed=false→취소). [US-3.4]
- `approveStart(adminId, meetingId) -> void` — ②: 시작대기→진행중. 409 on invalid. [US-6.1]
- `completeMeeting(adminId, meetingId) -> void` — ③: 전 세션 종료된 진행중 모임을 관리자가 직접 완료 처리(멘토 신청 없음, rev3). **전제조건("전 세션 종료")은 C4(SessionService) read로 확인** — 관리자 완료 오케스트레이션이 C4 확인 후 호출하거나 C2가 C4 read 포트에 의존(ADR-007, component-dependency R-2). 미완료/중복 409. 쓰기(완료 상태)는 C2 단일 소유. [US-7.3]

## C3. Enrollment

### EnrollmentService
- `apply(menteeId, meetingId) -> EnrollmentResponse` — 선착순, 정원마감/중복 시 거부(마감 안내/409). [US-3.2]
- `cancel(menteeId, meetingId) -> void` — 모집중·시작대기(②전) 허용, ②후 거부. [US-3.3]
- `listApplicants(mentorId, meetingId) -> ApplicantResponse[]` — 멘토용(권한 403 경계). [US-2.3]
- `listMyEnrollments(menteeId) -> EnrollmentStatusResponse[]` — 멘티 현황(상태·다음 액션). [US-3.5]

## C4. Session/Attendance

### SessionService
- `addSession(mentorId, meetingId, SessionCreateRequest{week, datetime}) -> SessionResponse` — 진행중 모임, 주차당 복수. [US-6.2]
- `updateSession(mentorId, sessionId, {datetime}) -> SessionResponse` — 일정 변경(멘티 현황 반영). [US-6.2]
- `listSessions(meetingId) -> SessionResponse[]` — 멘티/멘토 일정. [US-3.5/US-6.2]

### AttendanceService
- `checkIn(menteeId, sessionId) -> AttendanceResponse` — 유효 시간창 내만(밖=거부), 세션별 1회 멱등, 비참여자 403. [US-6.3]
- `getMyAttendance(menteeId, meetingId) -> {attended, totalScheduled, rate}` — 세션 기준. [US-7.4]

### CompletionService
- `computeCompletion(meetingId) -> MenteeCompletion[]` — 각 멘티 (a/S)≥0.80 정수비교(a*100≥80*S)로 수료후보 판정. [US-7.1]
- `approveMenteeCompletion(adminId, meetingId, menteeId) -> void` — ④: 수료후보를 관리자 승인→수료확정. 미충족/중복 409. [US-7.2]

## C5. Content

### PostService
- `createPost(mentorId, meetingId, PostCreateRequest{week, body}) -> PostResponse` — 본문(첨부 0개 가능). [US-4.1a]
- `listPosts(meetingId, requesterId) -> PostResponse[]` — 참여자만(비참여 403). [US-4.2]

### AttachmentService
- `upload(mentorId, postId, file) -> AttachmentResponse` — 형식 화이트리스트 외 400, 상한 초과 거부, BLOB 저장+메타. 스트리밍. [US-4.1b]
- `download(requesterId, attachmentId) -> stream` — 참여자 권한. [US-4.2]

### NoticeService
- `postNotice(mentorId, meetingId, NoticeRequest{body}) -> NoticeResponse` [US-4.3]

## C6. Messaging

### MessageService
- `send(senderId, recipientId, body) -> MessageResponse` — 권한 경계(멘토=자기 모임 멘티, 관리자=전원), 위반 403. [US-5.1]
- `listThreads(userId) -> ThreadSummary[]` / `getThread(userId, threadId) -> Message[]`
- `unreadCount(userId) -> int` — 인앱 뱃지(폴링). [US-5.1]

## C7. Survey/Feedback

### PreSurveyService
- `submitAnswers(menteeId, meetingId, Answer[]) -> void` — ②시작 후만(② 전 거부/비노출). [US-3.6]
- `getAnswers(requesterId, meetingId, menteeId) -> Answer[]` — 멘토/관리자 열람. [US-3.6/US-2.3]

### FeedbackService
- `submitFeedback(menteeId, meetingId, FeedbackRequest) -> void` — 진행/완료 참여 멘티. [US-8.1]
- `listFeedback(requesterId, meetingId) -> Feedback[]` — 멘토(자기 모임)·관리자 열람, 타모임 멘토 403. [US-8.2]

## C8. Admin/Monitoring (read 계층)

### AdminQueryService
- `getApprovalQueues(adminId) -> {creation[], recruitConfirm[], start[], meetingComplete[], menteeComplete[]}` — 4지점(①개설·②시작·③모임완료·④멘티수료) + 모집 확정 대기 집계. (③=관리자 직접 모임 완료, rev3) [US-9.1]
- `getMonitoring(adminId) -> MeetingMonitorRow[]` — 모임별 상태·출석율(세션 기준)·수료 진행. [US-9.2]
- (집계 지표는 범위 밖 — US-9.3 Won't)

## 공통 에러 처리 (전 메서드 상속, CC-1)

- 검증 실패 400 / 인증 401 / 인가 403 / 미존재 404 / 상태전이·중복 409. 본문 `{code,message,details}`. 전역 `@RestControllerAdvice`(team-practices).

## Assumptions & Open Questions

- 상세 비즈니스 규칙(경계값·전이 조건·유효 시간창·미응답 처리)은 functional-design.
- 페이지네이션·정렬 등 목록 파라미터는 functional-design/구현에서 구체화.
