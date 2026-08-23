# Code Summary — Bolt 1 Walking Skeleton (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). 승인된 code-generation-plan.md 실행 결과. Bolt 1 = U1 Contracts&Kernel + U2 Auth&Shell + U3 Meeting(최소 슬라이스). 애플리케이션 코드는 워크스페이스 루트(/backend·/frontend·/contracts). -->

## 관통 목표 달성

사번 가입 → 로그인 → (멘토) 모임 개설 → (관리자) ① 개설 승인 → 모집중(RECRUITING) 목록 노출 — 백엔드 Testcontainers 통합 테스트로 end-to-end 실증, 프론트 화면으로 조작 가능.

## 검증 결과

### 백엔드 (`/backend`, Spring Boot 3.x · Java 21 · Gradle)
- `./gradlew check` — BUILD SUCCESSFUL, **테스트 69개 전부 통과**, JaCoCo **line coverage 89.6%**(80% floor 통과), Spotless(google-java-format)·Checkstyle 통과.
- `./gradlew bootJar` — 실행 가능한 fat jar 생성.
- Testcontainers PostgreSQL 16 + Flyway + `ddl-auto=validate` 컨텍스트 기동 성공.

### 프론트엔드 (`/frontend`, React+TS+Vite)
- `npm run build`(`tsc -b && vite build`) — 타입 에러 0.
- `npm run test -- --run` — **59 테스트 / 14 파일 전부 통과**.
- coverage — line 95.2% · branch 84.7% · function 84.3% (80% floor 통과).
- `npm run lint` — 에러 0.

## 생성 파일 (요약)

### 백엔드
- **build/설정:** `backend/{settings.gradle,build.gradle,gradlew(.bat),gradle/wrapper/*}`, `backend/config/checkstyle/checkstyle.xml`
- **kernel(U1):** `domain`(Role/MeetingStatus/CompletionStatus), `error`(ErrorPayload/ErrorCodes/DomainException+5하위/GlobalExceptionHandler), `security`(Principal/@AuthPrincipal/ArgumentResolver), `web`(PageResponse/PageRequestFactory), `config`(JacksonConfig/WebConfig/PasswordEncoderConfig)
- **auth(U2):** entity(User/Profile/Session), repository×3, dto×6, service(AuthService/UserService), web(AuthController/UserController/SessionAuthInterceptor)
- **meeting(U3, 최소):** entity(Meeting/SurveyQuestion), repository×2, dto×5, service(MeetingService/SurveyTemplateService/MeetingApprovalService), web(MeetingController/MeetingApprovalController)
- **resources:** application.yml·application-local.yml, Flyway V1__baseline/V2__auth/V3__meeting.sql
- **test(69):** 서비스 단위·@WebMvcTest·OpenApiContractTest·Testcontainers(Auth/Meeting IntegrationTest)

### 프론트엔드
- 셸/설정: Vite+React+TS, Tailwind, shadcn/ui 9종, ESLint(flat)+Prettier, vitest.config, .env.example(VITE_API_BASE)
- api: 단일 client.ts(인증 헤더·ErrorPayload→ApiError·401 처리), endpoint 모듈(auth/users/meetings/admin), errors.ts, session.ts, AuthProvider/useAuth
- routing: 3탭 셸, RequireAuth/RequireRole 가드
- features/auth: 로그인·가입(MENTOR/MENTEE 라디오, 클라 검증, 409 매핑)·내정보
- features/meetings: 모집중 목록·모임 개설(+사전설문 빌더)·관리자 ①승인/반려·멘토 허브(최소)
- test(59/14파일)

### 루트
`/contracts/openapi.yaml`, `docker-compose.yml`, `.env.example`, `README.md`(로컬 실행 절차), `.gitignore`(.env·빌드 산출물)

## 주요 구현 결정

- **세션 인증:** Spring Security는 bcrypt PasswordEncoder만, 필터체인 없음. 자체 `SessionAuthInterceptor`가 `Authorization: Bearer` 파싱→validateSession→Principal 주입, `@AuthPrincipal` 리졸버가 소비.
- **상태 전이:** `MeetingRepository.transitionStatus` 조건부 UPDATE(`WHERE status=:from`)로 T1(①)/T2(반려) 원자 처리, 0 rows→409 MEETING_INVALID_TRANSITION(이중 승인·경합 커버). T3~T6은 주석 자리표시(Bolt 2+).
- **enum:** DB varchar+CHECK, JPA `@Enumerated(STRING)`. **PG 배열**(interest_tags/options): Hibernate 6 `@JdbcTypeCode(SqlTypes.ARRAY)`.
- **에러:** DomainException 계층 → @RestControllerAdvice → ErrorPayload{code(UPPER_SNAKE),message(한국어),details}. 로그인 실패 열거 방지(동일 401).
- **계약 테스트:** swagger-parser로 openapi.yaml 파싱 후 응답 DTO 직렬화가 스키마와 일치 검증.
- **FE 계약 정합(태스크 설명과 실제 계약 차이 3건은 계약 우선):** logout 204 No Content, MeetingResponse에 createdAt 없음, MeetingSummary에 모집기간 없음.

## 계획 대비 편차

- 승인 큐/내 모임 목록 조회 엔드포인트가 Bolt 1 계약에 없어, 관리자 승인·멘토 허브 화면은 `getMeeting(id)` 기반 ID 조회 최소 흐름으로 구성(자리표시 주석). → Bolt 8(Admin)·Bolt 2에서 승인 큐·listMyMeetings 확장.
- U3 상태 전이는 T1/T2만 구현(②/③/모집확정은 Bolt 2+).

## Bolt 2+ 이월

U3 잔여 전이(②/③/모집확정·문항 게이팅·운영 허브 read 조합), U4~U9 전체, CI/CD·배포·운영(project.md Scope Overrides로 이번 구현 범위에서 제외).

## 로컬 실행

1. `docker-compose up -d`(PostgreSQL) — `.env`는 `.env.example` 복사 후 값 주입(커밋 금지)
2. `cd backend && ./gradlew bootRun`
3. `cd frontend && npm install && npm run dev` (VITE_API_BASE 기본 http://localhost:8080)

(이 환경 Rancher Desktop에서 통합 테스트 실행 시 README의 DOCKER_HOST/DOCKER_API_VERSION/TESTCONTAINERS_RYUK_DISABLED 참고.)
