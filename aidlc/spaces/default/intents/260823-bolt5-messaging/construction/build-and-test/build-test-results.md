# Build & Test Results — Bolt 5 Messaging (learnKK)

<!-- build-and-test 산출물(Step 10 실제 실행 결과). 이 세션 실측(2026-08). 상류: bolt5-messaging code-generation-plan.md · code-summary.md. -->

## 실행 환경

- Java 21 + Gradle 8.10.2, Node/npm(Vitest 2.1.9 / Vite 5). Docker: Windows/Rancher Desktop(Testcontainers JNA 미가용). Postgres 16(docker-compose), Flyway V1~V5.

## 백엔드 (`/backend`)

- **컴파일/정적검사**: `compileJava`/`compileTestJava` OK, `spotlessCheck` + `checkstyleMain` + `checkstyleTest` clean.
- **테스트(비통합)**: `com.learnkk.{messaging,contract,meeting,enrollment.service,enrollment.web,auth,kernel}.*` → **BUILD SUCCESSFUL, 전부 통과**.
  - `MessageServiceTest` 18건(send 경계 403/400/404·성공, getThread 403/404/멱등, unreadCount, listThreads, listRecipients).
  - `MessageControllerTest` 9건(201/401/403/400 라우트 관통).
  - `OpenApiContractTest` — messaging 스키마 4종(MessageResponse·ThreadSummaryResponse·UnreadCountResponse·RecipientResponse) 정합 포함 통과.
- **커버리지(JaCoCo LINE)**: covered **678 / 783 → 86.6%**. `jacocoTestCoverageVerification`(≥80% floor) **BUILD SUCCESSFUL**.

## 프론트엔드 (`/frontend`)

- **빌드**: `npm run build`(`tsc -b && vite build`) → 타입 에러 0, `dist/` 산출(index js ~299KB, gzip 97KB), built in ~7s.
- **테스트**: **91 테스트 / 19 파일 전부 통과**(messaging: MessagesPage 4, ThreadView 3, AppShell 2; 기존 회귀 없음 — AppRouter.test는 unread-count 폴링 반영해 수정).
- **커버리지**: 전체 **94.87%** stmts. messages.ts 100%, features/messaging 91.9%(MessagesPage 88.6%, ThreadView 94.11%), AppShell 100%.
- **Lint/Format**: `eslint` 신규 파일 0건, `prettier --check` 통과.

## 권한 경계 가설 실증 (DoD)

Testcontainers 미가용으로 통합 관통 대신, **전체 MockMvc 체인(SessionAuthInterceptor → GlobalExceptionHandler)** 을 통과하는 웹 테스트 + 서비스 단위로 실증:

| 시나리오 | 결과 |
|---|---|
| 멘토 → 자기 모임 활성(APPLIED) 멘티 발신 | 성공(스레드 생성/재사용) ✅ |
| 멘토 → 무관 멘티 / 멘티 → 무관 멘토 | 403 MESSAGING_FORBIDDEN ✅ |
| 멘티 → 신청 모임 멘토 발신 | 성공 ✅ |
| 관리자 → 전원 / 전원 → 관리자 | 성공 ✅ |
| 자기 자신 발신 / 빈 본문 | 400 MESSAGING_SELF / VALIDATION_FAILED ✅ |
| 미존재 상대 | 404 MESSAGING_RECIPIENT_NOT_FOUND ✅ |
| 비참여자 스레드 열람 | 403 ✅ |
| 무토큰 접근(`/api/messages/**`) | 401 ✅ |
| 스레드 유일성 + 멱등 확인 처리 | 단위/통합 지시서 커버(통합은 Docker 환경) ✅ |

## 실패·조치

- 코드 결함 실패 0. 리뷰(§12a) 지적 1건(스레드 생성 경합→500) 수정: `DataIntegrityViolationException` 캐치 후 재조회(EnrollmentService/AuthService 패턴). 통합 테스트 미실행은 환경 원인 — 존치.

## 알려진 제한

- 통합 테스트는 Docker 접근 가능 환경 필요. deployment-ready 아님(CI/CD·operation은 스코프 SKIP).

## 라이브 E2E — 실행 앱 + 실제 PostgreSQL (Testcontainers 대체 실증)

Testcontainers 미가용 대체로 실제 스택(docker-compose Postgres :5434 + bootRun :8082, Flyway V5 적용) 기동 후 REST API로 messaging 경계를 관통 검증. 사용자 시드 후 관리자는 SQL role 승격(가입은 ADMIN 거부). **전 시나리오 통과.**

| 시나리오 | 결과 |
|---|---|
| 모임 개설 → 관리자 승인(RECRUITING) → 멘티 신청 | 201 / 200 / 201 ✅ |
| **멘티 → 멘토(신청 모임) 발신** | 201, thread 생성 ✅ |
| **멘토 → 멘티 답장** | 201, **동일 threadId**(정규화 쌍 유일성) ✅ |
| 멘토 미확인 수 = 1 → 스레드 열람 → 미확인 = 0 | 멱등 확인 처리 ✅ |
| **무관 멘티(stranger) → 멘토 발신** | **403 MESSAGING_FORBIDDEN**(DoD 가설 실증) ✅ |
| 관리자 → 멘티 발신 | 201 ✅ |
| 멘티 → 자기 자신 발신 | 400 MESSAGING_SELF ✅ |
| 멘티 recipients 조회 | 200, 허용 상대(멘토) 포함 ✅ |

- **핵심 가설 실증**: 권한 경계(멘토=자기 모임 활성 멘티 / 멘티=신청 모임 멘토 / 관리자=전원)가 실제 DB·전체 스택에서 403으로 강제됨. 스레드 유일성·미확인 멱등도 실앱에서 확인.
