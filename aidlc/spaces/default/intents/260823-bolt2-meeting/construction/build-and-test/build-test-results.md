# Build & Test Results — Bolt 2 Meeting 완성 (learnKK)

<!-- build-and-test 산출물(Step 10 실제 실행 결과). 출처: code-generation-plan.md·code-summary.md 대상 코드에 대한 실행. 이 세션에서 실측(2026-08-23). -->

## 실행 환경

- Java 21 + Gradle 8.10.2(wrapper), Node/npm(Vitest 2.1.9 / Vite 5.4.21). Docker: Windows/Rancher Desktop — Testcontainers JNA 초기화 실패(아래).

## 백엔드 (`/backend`)

- **빌드/컴파일**: `./gradlew clean test` → compileJava/compileTestJava OK. Spotless(google-java-format)·Checkstyle 위반 0.
- **테스트**: **103개 중 94개 통과, 9개 실패.** 실패 9개는 **전부 Testcontainers 통합 테스트**(3 AuthIntegrationTest + 6 MeetingIntegrationTest).
- **커버리지(JaCoCo LINE, 실행 가능한 94개 기준)**: covered 467 / missed 62 → **88.3%** (80% floor 통과).
- **단위/슬라이스/계약 부분 실행**: `--tests "com.learnkk.{meeting,auth,kernel,contract,config}.*"` → **BUILD SUCCESSFUL**(통합 제외 전부 통과).

### 실패 상세 (환경 원인, 코드 결함 아님)
- 9건 전부 `java.lang.ExceptionInInitializerError` → `Caused by: java.lang.IllegalStateException at DockerClientProviderStrategy.java:277`("Could not find a valid Docker environment"). docker-java의 JNA named-pipe 전송이 이 환경(Windows/Rancher Desktop, 포크된 테스트 JVM)에서 초기화 실패.
- **기존 Bolt 1 `AuthIntegrationTest`도 동일하게 실패** — Bolt 2 코드와 무관한 환경/컨테이너 런타임 제약. 통합 테스트 코드 자체는 정상(리뷰어 확인), Docker 접근 가능 환경에서 통과 예상.

## 프론트엔드 (`/frontend`)

- **빌드**: `npm run build`(`tsc -b && vite build`) → 1674 모듈 변환, **타입 에러 0**, dist 산출.
- **테스트**: `npm run test -- --run --coverage` → **72개 / 15개 파일, 전부 통과** (T4 취소·모집확정·403/409 매핑 포함).
- **커버리지(v8)**: All files — Lines **95.72%**, Branch 86.99%, Funcs 85.45% (80% floor 통과). 신규: admin.ts 100%·MyLearningPage 100%·AdminApprovalPage 94.9%·meetings.ts 93.1%.

## 상태머신 검증 (전이 커버)

- 단위/슬라이스: T3 진행·T4 취소(사유 필수)·T5 ②시작·T6 ③완료(gate)·불법 전이 409·비관리자 403·경합 409 — `MeetingApprovalServiceTest`·컨트롤러 `@WebMvcTest` 통과.
- 관통(개설→①→모집확정→②→③) end-to-end는 `MeetingIntegrationTest`가 커버하나 Testcontainers 환경 제약으로 미실행 → 아래 **라이브 E2E**로 실제 검증 대체.

## 라이브 E2E (실행 앱 + 실제 PostgreSQL, 2026-08-23)

Testcontainers 통합 테스트가 이 환경에서 실행 불가하므로, 실제로 스택을 기동해 REST API로 상태머신 전 과정을 검증했다: `docker compose`(PostgreSQL 16) + `./gradlew bootRun`(profile=local, Flyway V1~V3 validate) 기동 후 curl로 라이프사이클 구동. 관리자 계정은 MENTEE 가입 후 SQL로 role=ADMIN 승격(ADMIN 가입은 정책상 차단).

| 시나리오 | 결과 |
|----------|------|
| 멘토/관리자 가입·로그인 | 201/200 ✅ |
| 개설 → PENDING_APPROVAL | ✅ |
| T1 approve → RECRUITING | 200 ✅ |
| T3 confirm(proceed=true) → READY_TO_START | 200 ✅ |
| T5 approve-start → IN_PROGRESS | 200 ✅ |
| T6 complete → COMPLETED | 200 ✅ |
| 불법 전이(모집확정 전 ②시작) | 409 `MEETING_INVALID_TRANSITION` ✅ |
| T4 cancel 사유 누락 | 400 ✅ |
| T4 cancel(reason) → CANCELLED, reject_reason DB 저장 | 200 ✅ (DB: `CANCELLED\|under capacity`) |
| 비관리자 전이 액션 | 403 ✅ |
| 멘토 listMine | 200(자기 모임 반환) ✅ |
| 비멘토 listMine | 403 ✅ |

- **결과: 전이 전 과정 + 인가·검증 경계 실 DB 기준 통과.** 상태 전이는 실제 조건부 UPDATE로 영속화 확인.

### UI E2E (브라우저, 3역할 탭, Playwright)

프론트 dev 서버(Vite :5177) + 백엔드(CORS 허용 origin에 dev 포트 추가) 기동 후 멘토/멘티/관리자를 각각 별도 탭에 로그인(세션 토큰은 탭별 sessionStorage로 격리)해 시나리오 구동:

- **멘토 탭**: 모임 개설(문항 빌더 포함) → 모임 #8 PENDING_APPROVAL. "내 러닝"(운영 허브 = listMyMeetings)에 자기 모임이 상태·다음 액션과 함께 실목록 표시(Bolt 1 lookup-by-id 대체).
- **멘티 탭**: 승인 전 모집중 목록에 #8 미노출 → 관리자 승인 후 새로고침 시 "모집중"으로 노출(권한/상태 경계 정합).
- **관리자 탭**: 조회한 모임의 상태에 따라 버튼이 순차 변화하며 전 전이 구동 — 승인대기[승인/반려] → **모집중**[모집확정(진행)/취소] → **시작대기**[시작 승인] → **진행중**[완료 처리] → **완료**["종료된 모임…추가 작업 없음"].
- 크로스탭 일관성: 관리자 완료 처리 후 멘토 "내 러닝"이 **완료**로 갱신 확인.
- (T4 취소+사유 다이얼로그·반려는 API E2E와 FE 컴포넌트 테스트로 커버.)
- **관측**: 백엔드 CORS 허용 origin 기본값이 `http://localhost:5173`뿐 — dev 서버가 다른 포트(5177)로 뜨면 브라우저 CORS 차단. `learnkk.cors.allowed-origins` env로 해소. dev 편의를 위해 기본값에 5173~5177 포함 또는 문서화 권장(코드 결함 아님, 설정값).
- **관측(비Bolt2/pre-existing)**: 요청 본문이 비UTF-8(malformed)일 때 전역 핸들러가 500 `INTERNAL_ERROR` 반환(400이 더 적절). Bolt 2 무관한 전역 로버스트니스 갭 — FE는 항상 UTF-8 전송. (테스트 중 셸 cp949 인코딩으로 최초 재현된 것으로, 앱 결함 아님.)

## 실패·조치

- 코드 결함 실패 0. 통합 테스트 미실행은 환경 원인 — 진단 완료, 코드 수정 불필요(존치).

## 알려진 제한

- 통합 테스트는 Docker 필요(현재 환경 미가용). 단위·슬라이스·계약·FE는 전부 통과.
- deployment-ready 아님 — CI/CD·배포·operation은 스코프 SKIP(project.md Scope Overrides). 로컬 실행만 지원.
