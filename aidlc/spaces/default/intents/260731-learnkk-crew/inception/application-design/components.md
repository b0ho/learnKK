# Components — learnKK (런크크)

<!-- application-design 산출물(architect 리드 + aws-platform(로컬 전제)·design 관점). 출처: requirements.md(FR/NFR), user-stories(stories), team-practices. 모듈러 모놀리스(Q1) · 7개 도메인 모듈(Q2). 전부 로컬 단일 인스턴스. team-practices 3계층(Controller/Service/Repository)·Entity 비노출·DTO 경계를 각 모듈이 상속. -->

## 아키텍처 개요

- **스타일:** 모듈러 모놀리스 — 단일 Spring Boot 앱, 도메인 모듈(패키지) 경계로 분리. 로컬 단일 인스턴스·파일럿 규모(NFR2). 3인 병렬은 모듈 소유로 배분(team-practices). [Q1]
- **계층:** 각 모듈은 `Controller → Service → Repository` 3계층(team-practices). Controller는 Request/Response DTO만, Entity를 API 경계에 노출하지 않음. JSON camelCase / JPA snake_case.
- **프론트:** React + shadcn/ui, feature 기반 폴더 + 단일 API client 계층(Q6, refined-mockups design-system-mapping).
- **데이터:** 단일 PostgreSQL, 모듈별 테이블 소유, 교차 접근은 Service 경유. 첨부 BLOB. 마이그레이션 Flyway. [Q4]
- **공유 계약:** #1 OpenAPI(전 REST 엔드포인트), #2 DB 스키마, #3 도메인 타입(모임 상태머신·수료 상태 enum) — team-practices·stories 계약. #3은 shared 커널에 위치(아래).

## 컴포넌트(도메인 모듈) 목록

각 모듈은 자기 도메인의 Controller/Service/Repository/DTO/Entity를 소유한다. 모듈 간 호출은 상대 모듈의 **Service 인터페이스**를 통해서만(인-프로세스 동기, Q3).

### C0. Shared Kernel (공유 커널)
- **책임:** 도메인 타입 계약 #3 — 모임 상태 enum(개설신청/모집중/시작대기/진행중/완료/반려/취소), 멘티 수료 상태 enum(미수료/수료후보/수료확정), 공통 에러 스키마 `{code,message,details}`, 공통 값객체(사번 등), RBAC 역할 enum(MENTOR/MENTEE/ADMIN).
- **경계:** 순수 타입·상수·유틸만. 비즈니스 로직 없음. 전 모듈이 참조.
- **소유 근거:** interface 불일치 방지(team-practices 계약 #3의 단일 소유 위치).

### C1. Auth/User (인증·사용자)
- **책임:** 회원가입(닉네임·비밀번호·**사번**), 로그인·세션, RBAC 경계, 프로필(관심사 해시태그·소개), 사번 유일성 기반 중복계정 방지.
- **인터페이스(공개):** `AuthService`(회원가입/로그인/세션검증/로그아웃), `UserService`(프로필 조회·수정, 사용자 조회).
- **소유 데이터:** user(사번 유니크), profile, session.
- **Ref:** US-1.1~1.4, FR1.x, NFR6/NFR8.

### C2. Meeting (모임·개설·승인·상태머신)
- **책임:** 모임 개설(기본정보·사전설문 문항 빌더), 상태머신 전이 소유(①개설 승인, 모집확정, ②시작 승인, ③모임 완료 관리자 직접), 반려/취소.
- **인터페이스:** `MeetingService`(개설/조회/목록), `MeetingApprovalService`(①승인·반려, 모집확정, ②시작, ③완료 — 관리자 액션), `SurveyTemplateService`(사전설문 문항 CRUD).
- **소유 데이터:** meeting, survey_question(문항 틀).
- **소유 근거:** 상태머신(#3 enum은 C0, 전이 규칙·집행은 C2).
- **Ref:** US-2.1a/2.1b/2.2/2.3, US-3.4, US-6.1, US-7.3, FR2.x, FR3.4, FR7.2.

### C3. Enrollment (신청·모집)
- **책임:** 멘티 신청(선착순, 정원·중복 제어), 신청 취소, 모집 확정 연계.
- **인터페이스:** `EnrollmentService`(신청/취소/정원조회/신청자 목록).
- **소유 데이터:** enrollment(모임-멘티, 상태).
- **협력:** Meeting(모집중 상태·정원), Survey(응답은 ②후 C7).
- **Ref:** US-3.1(목록은 Meeting 조회)·US-3.2/3.3/3.5, FR3.x.

### C4. Session/Attendance (세션 일정·출석·수료 판정)
- **책임:** 멘토 세션 일정(날짜·시간, 주차당 복수, 변경), 멘티 팝업 출석(세션별 self check-in, 유효 시간창 판정 — 스케줄러리스), 출석율 산출(출석 세션/전체 예정 세션), 80% 수료 자동 판정.
- **인터페이스:** `SessionService`(세션 생성·변경·조회), `AttendanceService`(출석 체크·조회), `CompletionService`(출석율 산출·수료후보 자동 판정).
- **소유 데이터:** session(모임-주차-일시), attendance(세션-멘티).
- **소유 근거:** 시간 판정은 요청 시점 비교(Q5). 멘티 수료 확정(④)은 관리자 액션이나 판정 로직은 여기.
- **Ref:** US-6.2/6.3, US-7.1/7.2(자동판정), US-7.4, FR6.x, FR7.1.

### C5. Content (게시글·자료·공지)
- **책임:** 주차 게시글(본문 + 선택 첨부 0개 이상), 파일 첨부(BLOB 저장·형식/크기 검증), 공지.
- **인터페이스:** `PostService`(게시글 CRUD·열람 권한), `AttachmentService`(업로드·다운로드 스트리밍·메타데이터), `NoticeService`(공지).
- **소유 데이터:** post, post_attachment(BLOB+메타), notice.
- **Ref:** US-4.1a/4.1b/4.2/4.3, FR4.x.

### C6. Messaging (쪽지)
- **책임:** 쪽지 스레드(멘토↔멘티, 관리자↔멘토/멘티), 인앱 미확인 뱃지(폴링), 권한 경계.
- **인터페이스:** `MessageService`(전송·스레드 조회·미확인 수).
- **소유 데이터:** message_thread, message.
- **노트:** 채팅형 전환은 미확정(OQ2) — 스레드형 기본.
- **Ref:** US-5.1, FR5.x.

### C7. Survey/Feedback (사전설문 응답·과정 설문)
- **책임:** 사전설문 응답(②시작 후 수집·열람), 과정 설문(종료 후 피드백 제출), 멘토·관리자 피드백 열람.
- **인터페이스:** `PreSurveyService`(응답 제출·열람 — ②후 게이팅), `FeedbackService`(과정 설문 제출·열람).
- **소유 데이터:** survey_answer(사전설문 응답), feedback(과정 설문).
- **협력:** 문항 틀은 Meeting(C2 survey_question), 응답은 C7.
- **Ref:** US-3.6, US-8.1/8.2, FR3.6, FR8.x.

### C8. Admin/Monitoring (관리자 조회 — cross-cutting, 얇은 조회 계층)
- **책임:** 승인 큐 집계(①②③④ + 모집 확정 대기), 운영 현황 모니터링(모임 상태·출석율·수료 진행). 실제 승인 액션은 각 도메인 Service(주로 C2/C4)로 위임 — 여기는 **조회/집계 read 계층**.
- **인터페이스:** `AdminQueryService`(승인 큐·현황 조회).
- **소유 데이터:** 없음(타 모듈 Service read 조합). 집계 지표(충족률·수료율 등)는 이번 범위 밖(US-9.3 Won't).
- **Ref:** US-9.1/9.2, FR9.1.

## 프론트엔드 컴포넌트 구조 (React, feature 기반)

- **api/**: 단일 API client(fetch 래퍼, 에러 스키마 해석, 인증 헤더). 도메인별 endpoint 모듈.
- **features/**: auth, meetings, enrollment, sessions(출석), content, messaging, survey, admin — 각 feature에 화면·컴포넌트·훅.
- **components/ui/**: shadcn/ui 기반 공통 컴포넌트(Badge, Card, Dialog, Form, Progress 등, design-system-mapping).
- **routes/**: 3탭(모임/내 러닝/내정보) + 상세·모달 라우팅. 내 러닝은 역할 적응형.
- 상태: 서버 상태는 fetch 캐시 계층, 전역 UI 상태 최소.

## 컴포넌트 경계·소유 원칙

- 모듈은 자기 테이블만 소유. 타 모듈 데이터는 그 모듈 Service를 통해 접근(직접 테이블 접근 금지).
- Entity는 모듈 내부에 갇힘 — 모듈 간·API 경계는 DTO/도메인 타입(C0)만 통과(team-practices).
- 상태머신 전이는 C2가 단일 소유(집행), enum은 C0(정의). 불법 전이는 409(stories CC-1).
- 3인 병렬 배분 후보: 예) A=C1+C6, B=C2+C3+C8, C=C4+C5+C7 (delivery-planning에서 확정).

## Assumptions & Open Questions

- 모듈 간 순환 의존 회피는 component-dependency.md에서 검증.
- 사번 형식·출석 유효 시간창·세션 변경 통지·반려 사유·사전설문 미응답 처리는 functional-design(requirements A5/A6/OQ7).
- 3인 모듈 배분은 delivery-planning 확정(위는 후보).
## Review

**Reviewer:** aidlc-architecture-reviewer-agent — iteration 2 (re-review)

### B1 (blocking, iter1) — RESOLVED

The previously-unacknowledged C2→C4 read dependency behind the ③완료 precondition ("전 세션 종료") is now consistently modeled across all three artifacts:

- **component-dependency.md** — matrix now uses `Y`(write/enforce)/`R`(read-only)/`-` notation and shows **C2→C4 `R`(세션 종료 read)** explicitly, alongside C2→C3 `R`(신청자 read) and C4→C2 `R`(진행중 상태 read). The cyclic-dependency section is rewritten into R-1 (C2↔C3) and R-2 (C2↔C4) read cross-references, each resolved via controller-level aggregation or a shared read port. The stale "C2→C4 없음/단방향" claim is explicitly retracted ("...이전 서술은 정정됨").
- **component-methods.md** — `MeetingApprovalService.completeMeeting` now states the precondition is verified via **C4 (SessionService) read** (orchestration checks C4 then calls completeMeeting, or C2 depends on a C4 read port), while the write (completion state) stays C2-owned. Cross-refs ADR-007 and component-dependency R-2 resolve.
- **decisions.md** — ADR-007 broadened to "Meeting↔(Enrollment, Session) read 상호참조" covering both R-1 and R-2 (Status Proposed), references resolve to R-1/R-2, C2/C3/C4/C8, and completeMeeting.

No residual self-contradiction. **Write-transition (`Y`) graph is acyclic** (all `Y` edges point toward C0/C1/C2/C3 base; the only C2↔C3 and C2↔C4 edges are `R`; C4→C3 has no reciprocal C3→C4). **C0 remains a leaf** (depends on nothing; universally referenced).

### Suggestions — applied

- **S1** (C2↔C3 aggregation): clarified in R-1 and ADR-007 (a)/(b)/(c) — controller/client composition, shared read port, or C8 read layer. ✓
- **S2** (ADR reversibility): ADR Index gained a Reversibility column with per-ADR ratings. ✓
- **S3** (bytea vs LO): ADR-004 now caveats that bytea is typically fully memory-loaded and defers the bytea/LO streaming choice to functional-design. ✓
- **S5** (matrix ASCII): matrix symbols are ASCII-friendly `Y`/`R`/`-`. ✓

### Regression check — clean

Coverage (US/FR refs), rev2 (mentor-scheduled sessions, popup attendance, scheduler-less), rev3 (③ = admin-direct completion; `meetingComplete[]` rename applied in AdminQueryService, comment consistent), rev4 realities, team-practices (3-layer, Entity non-exposure, DTO boundary, Flyway, docker-compose), NFR mapping, ADR quality (context/decision/consequences/alternatives/references), and sensor-relevant sections all intact.

### Minor (non-blocking, defer to functional-design)

- Matrix cell **C4→C3 is labeled `Y`("참여자=수료대상")**, but CompletionService only *reads* enrollment participants; under the new `Y`/`R` legend this reads as `R`. No cycle either way (C3 has no dependency on C4), so it is a labeling refinement, not a structural defect.
- C8→C7 `R` is plausible (admin viewing survey/feedback) but not explicitly justified in the C8 responsibility text; a one-line note would tighten traceability.

Verdict: READY
