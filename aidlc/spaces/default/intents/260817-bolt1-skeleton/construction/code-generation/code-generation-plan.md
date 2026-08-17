# Code Generation Plan — Bolt 1 Walking Skeleton (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 1 = U1 Contracts&Kernel + U2 Auth&Shell + U3 Meeting(최소 슬라이스). 상속 설계: 260731-learnkk-crew intent의 application-design(ADR-001~007)·U1/U2/U3 functional-design·nfr-requirements. 규칙: team.md(monorepo·3계층·계약우선·test-alongside·80% floor)·project.md(스택 lock·bcrypt·camelCase/snake_case·전역 에러 스키마)·construction.md. -->

## 목표 (Definition of Done — bolt-plan Bolt 1)

3계약(#1 OpenAPI · #2 Flyway 스키마 · #3 도메인 enum) 선고정 위에서 다음을 end-to-end 관통한다:

> 사번 가입 → 로그인 → (멘토) 모임 개설 → (관리자) ① 개설 승인 → 모집중 목록 노출

- 테스트 test-alongside, 백엔드/프론트 각각 80% line coverage floor + 도메인 분기(①승인·불법 전이 409·사번/닉네임 중복 409) 시나리오 커버.
- 아키텍처 가설 증명: 모듈러 모놀리스 + 계약 우선 + 3계층이 실제 관통 동작하고, 3인이 이 계약 위에서 병렬 착수 가능.

## 스택 (상속 확정)

- **Monorepo:** `/backend`(Spring Boot, Java 21) · `/frontend`(React+TS+Vite) · `/contracts`(OpenAPI 스펙) · 루트 `docker-compose.yml`·`.env.example`.
- **Backend:** Spring Boot 3.x, Java 21, Gradle. 3계층(Controller→Service→Repository). JPA(Hibernate) snake_case 물리 네이밍, Jackson camelCase. Flyway. Spring Security(bcrypt PasswordEncoder만 사용, 세션은 자체 토큰). 전역 `@RestControllerAdvice`.
- **DB:** PostgreSQL(로컬 docker-compose). enum = varchar + CHECK.
- **Frontend:** React + TypeScript + Vite, shadcn/ui, 단일 API client(fetch 래퍼).
- **Test:** BE = JUnit 5 + Spring Boot Test + MockMvc(`@WebMvcTest`) + Mockito + Testcontainers(PostgreSQL) + JaCoCo. FE = Vitest + React Testing Library + `@testing-library/user-event`.
- **Lint/Format:** BE = Spotless(google-java-format) + Checkstyle. FE = ESLint(@typescript-eslint) + Prettier.

패키지 루트: `com.learnkk` (모듈 = `kernel`, `auth`, `meeting`).

---

## 실행 단계 (layer-by-layer, 의존 → 의존자 순)

### Step 1: 프로젝트 스캐폴드 + 로컬 실행 환경
- [ ] Monorepo 구조 생성: `/backend`, `/frontend`, `/contracts`
- [ ] `/backend` Gradle Spring Boot 프로젝트(`build.gradle`, `settings.gradle`, `gradlew`) — deps: web, data-jpa, validation, security(crypto), flyway, postgresql, jackson; test: spring-boot-starter-test, testcontainers(postgresql, junit-jupiter)
- [ ] 루트 `docker-compose.yml`(PostgreSQL 16) + `.env.example`(DB 자격증명·세션 시크릿 placeholder) + `.gitignore`에 `.env`·빌드 산출물 추가
- [ ] `application.yml` + `application-local.yml`(env/profile 주입, 시크릿 비커밋 — project.md Forbidden 준수)
- 추적: 아키텍처 기반(ADR-001 모듈러 모놀리스, team.md Deployment docker-compose)

### Step 2: U1 Shared Kernel — 도메인 타입 계약(#3) + 경계 규약
- [ ] enum: `MeetingStatus`(PENDING_APPROVAL/RECRUITING/READY_TO_START/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED), `CompletionStatus`(NOT_COMPLETED/COMPLETION_CANDIDATE/COMPLETED), `Role`(MENTOR/MENTEE/ADMIN)
- [ ] `ErrorPayload{code,message,details}` + 에러 코드 상수(UPPER_SNAKE `<DOMAIN>_<REASON>`)
- [ ] `Principal{userId, role}` 공유 타입 + `@AuthPrincipal` 파라미터 리졸버(컨트롤러 주입)
- [ ] Pagination 요청/응답 래퍼(`PageResponse<T>{content,page,size,totalElements,totalPages}`)
- [ ] 도메인 예외 계층(`DomainException` + 상태코드 매핑용 하위: Validation400/Unauthorized401/Forbidden403/NotFound404/Conflict409)
- [ ] 전역 `@RestControllerAdvice` — 도메인 예외/검증 예외 → CC-1(400/401/403/404/409) + ErrorPayload
- [ ] Jackson config(camelCase 명시), JPA physical naming(snake_case) config
- 추적: U1 domain-entities(#3 enum·ErrorPayload·Principal·Pagination), U1 business-rules BR-U1-1/3/4/5/6, project.md Mandated(camelCase/snake_case·전역 에러 스키마)

### Step 3: DB 스키마 계약(#2) — Flyway 마이그레이션
- [ ] `V1__baseline.sql` — 공통 규약(주석), enum CHECK 헬퍼 규약
- [ ] `V2__auth.sql` — `users`(id, nickname unique, password_hash, employee_no unique, role varchar+CHECK, created_at, updated_at), `profiles`(user_id PK/FK, interest_tags text[], intro), `sessions`(token PK, user_id FK, role, created_at, expires_at, revoked_at)
- [ ] `V3__meeting.sql` — `meetings`(id, mentor_id FK, title, topic, weeks, recruit_start, recruit_end, capacity, format, initial_content, status varchar+CHECK default PENDING_APPROVAL, reject_reason, created_at, updated_at), `survey_questions`(id, meeting_id FK, order_no, text, type, options, required)
- 추적: U1 domain-entities(#2 baseline), U2/U3 domain-entities(테이블), ADR-003(Flyway)

### Step 4: U2 Auth 도메인 — 백엔드 (C1)
- [ ] Entity: `User`, `Profile`, `Session`(JPA, API 비노출) + Repository(Spring Data JPA)
- [ ] DTO: `SignupRequest{nickname,password,employeeNo,role}`(role∈{MENTOR,MENTEE}), `LoginRequest`, `SessionResponse{token,role}`, `UserResponse`, `ProfileResponse`, `ProfileUpdateRequest`
- [ ] `AuthService`: signup(정규화·유일성 선검증·bcrypt·경합-안전 409, ADMIN 가입 400) / login(nickname 조회·bcrypt·실패 401 열거방지) / validateSession(만료·revoked 401) / logout
- [ ] `UserService`: getProfile / updateProfile(본인만 403·태그≤10·소개≤500 400)
- [ ] 세션 인증 인터셉터/필터 — 보호 라우트 전처리, `Principal` 요청 컨텍스트 주입
- [ ] Controller: `AuthController`(POST /api/auth/signup, /login, /logout), `UserController`(GET/PUT /api/users/me/profile) — DTO만
- 추적: US-1.1~1.4, W1~W4, BR-U2-1/1a/1b/2/3/4/5, project.md Mandated(bcrypt), FR1.x

### Step 5: U2 테스트 (test-alongside, Standard)
- [ ] `AuthService` 단위(Mockito): 정상 가입·중복 사번 409·중복 닉네임 409·ADMIN 가입 400·로그인 성공/실패 401·세션 만료 401 (5~8)
- [ ] `UserService` 단위: 프로필 조회/수정·본인 아님 403·상한 초과 400
- [ ] `AuthController` `@WebMvcTest`(MockMvc): 요청/응답 스키마·상태코드 매핑
- [ ] 통합(Testcontainers PostgreSQL): 가입→로그인→세션검증 end-to-end + 사번 unique 제약(경합 409)
- 추적: team.md Testing Posture(80% floor·분기 시나리오), construction.md(happy+2 edge)

### Step 6: U3 Meeting 도메인 — 백엔드 최소 슬라이스 (C2)
- [ ] Entity: `Meeting`, `SurveyQuestion` + Repository
- [ ] DTO: `MeetingCreateRequest{title,topic,weeks,recruitPeriod,capacity,format,initialContent}`, `MeetingResponse`, `MeetingSummary`, `SurveyQuestionDto[]`, `RejectRequest{reason}`
- [ ] `MeetingService`: createMeeting(MENTOR 403·검증 400·status=PENDING_APPROVAL) / getMeeting(404) / listRecruiting(RECRUITING만·페이지네이션)
- [ ] `SurveyTemplateService`: upsertQuestions(소유 멘토·IN_PROGRESS 이전만) / getQuestions
- [ ] `MeetingApprovalService`(ADMIN): approveCreation ①(T1 PENDING_APPROVAL→RECRUITING, 조건부 UPDATE·불법 409 MEETING_INVALID_TRANSITION) / rejectCreation(T2→REJECTED)
- [ ] Controller: `MeetingController`(POST /api/meetings, GET /api/meetings/{id}, GET /api/meetings?status=recruiting, PUT /api/meetings/{id}/questions), `MeetingApprovalController`(POST /api/admin/meetings/{id}/approve, /reject)
- [ ] 상태 전이 가드(전이표 T1/T2만 이번 Bolt, 나머지는 Bolt 2+ 주석 명시)
- 추적: US-2.1a/2.1b/2.2/3.1, W1~W3, BR-U3-1(T1/T2)/3/6/7, ADR-006(상태머신 소유)

### Step 7: U3 테스트
- [ ] `MeetingService` 단위: 개설 정상·비멘토 403·검증 400·listRecruiting 필터
- [ ] `MeetingApprovalService` 단위: ①승인 정상·잘못된 상태 409·반려·이중 승인 409(조건부 UPDATE)
- [ ] `MeetingController` `@WebMvcTest`
- [ ] 통합(Testcontainers): 개설→①승인→listRecruiting 노출 end-to-end
- 추적: team.md Testing Posture, ADR-006

### Step 8: API 계약(#1) + 계약 테스트 계층
- [ ] `/contracts/openapi.yaml` — Bolt 1 엔드포인트(auth/users/meetings/admin) 요청·응답 DTO·상태코드·ErrorPayload·SignupRequest.role 확장 반영
- [ ] API 계약 테스트: 주요 엔드포인트 응답이 OpenAPI 스키마와 일치 검증(응답 스키마 고정)
- 추적: team.md 계약 #1·계약 테스트 계층, BR-U2-1a(#1 반영 의무)

### Step 9: Frontend 앱 셸 스캐폴드 (C1 FE)
- [ ] Vite React TS 프로젝트, shadcn/ui 셋업(Button/Input/Form/Card/Badge/Dialog/Tabs 등), Tailwind
- [ ] `api/` 단일 API client — fetch 래퍼(인증 헤더 자동·ErrorPayload 해석 code→한국어 메시지·401 세션만료 처리)
- [ ] `routes/` — 3탭(모임/내 러닝/내정보) + 비인증(로그인/가입) 라우트, 역할 적응형 가드
- [ ] 세션 토큰 저장(sessionStorage), 인증 컨텍스트
- 추적: U2 business-logic-model FE 앱 셸, components.md FE 구조, NFR1(모바일 웹뷰)

### Step 10: Frontend 인증 화면 (auth feature)
- [ ] 로그인 화면(nickname/password, 실패 매핑), 가입 화면(사번/닉네임/비밀번호 + MENTOR/MENTEE 라디오, ADMIN 미노출, 클라이언트 검증 + 서버 409 매핑)
- [ ] 내정보(프로필 조회·수정·로그아웃), 역할 적응형 셸 렌더
- [ ] `data-testid` 부여(테스트 자동화)
- 추적: US-1.1/1.2/1.4, BR-U2-6, 접근성 CC-2

### Step 11: Frontend 모임 화면 (meetings feature — 최소 슬라이스)
- [ ] 모임 목록(멘티: RECRUITING 카드 목록·상태 뱃지), 모임 개설 화면(멘토: 기본정보 폼 + 사전설문 문항 빌더)
- [ ] 관리자 액션(개설신청 목록 → ①승인/반려 버튼), 멘토 운영 허브(자기 모임 목록·상태·다음 액션 — 최소)
- [ ] `data-testid` 부여
- 추적: US-2.1a/2.1b/2.2/3.1, U3 business-logic-model FE 화면

### Step 12: Frontend 테스트 (RTL + Vitest)
- [ ] 로그인/가입 폼 상호작용·검증·에러 매핑 테스트
- [ ] 모임 목록·개설 폼·관리자 승인 액션 테스트(API client mock)
- [ ] 단일 API client 단위(ErrorPayload 해석·401 처리)
- 추적: team.md Testing Posture(FE 80% floor)

### Step 13: 테스트·빌드 설정
- [ ] BE: JaCoCo 커버리지, Spotless/Checkstyle Gradle 연결
- [ ] FE: `vitest.config.ts`(coverage), ESLint/Prettier 설정(루트)
- 추적: team.md Code Style·Testing Posture

### Step 14: 문서
- [ ] 루트 `README.md` — 로컬 실행 절차(docker-compose up → backend gradle bootRun → frontend dev), 계약 위치, Bolt 1 범위/제외 명시
- [ ] 인라인 문서(핵심 서비스·전이 가드), OpenAPI가 API 문서 역할
- 추적: 인수인계

---

## 범위 밖 (Bolt 2+ 이월, 이번 미구현)

- U3: ②시작·③완료·모집확정·문항 ②후 게이팅·운영 허브 read 조합(U4/U8)·반려 사유 필수화
- U4~U9(신청/세션·출석/자료/쪽지/설문/관리자 모니터링) 전체
- CI/CD·배포·운영 인프라(후속 워크플로우)

## Assumptions

- Java 21 + Spring Boot 3.x + Gradle, Vite 기반 FE(→ Vitest) 채택(team-practices [assumption] 확정).
- 세션 TTL 12h, sessionStorage 토큰 저장(U2 [assumption]).
- enum 물리 표현 varchar+CHECK(U1 기본 가정).
- 관심사 태그 `text[]`(U2 [assumption]).
