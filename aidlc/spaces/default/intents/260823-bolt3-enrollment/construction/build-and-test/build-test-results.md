# Build & Test Results — Bolt 3 Enrollment (learnKK)

<!-- build-and-test 산출물(Step 10 실제 실행 결과). 이 세션 실측(2026-08-23). -->

## 실행 환경
- Java 21 + Gradle 8.10.2, Node/npm(Vitest 2.1.9 / Vite 5.4.21). Docker: Windows/Rancher Desktop(Testcontainers JNA 미가용). Postgres 16(docker-compose), Flyway V1~V4 적용.

## 백엔드 (`/backend`)
- **컴파일/정적검사**: compileJava/compileTestJava OK, Spotless+Checkstyle clean.
- **테스트(비통합)**: `com.learnkk.{enrollment,meeting,auth,kernel,contract}.*` → **BUILD SUCCESSFUL, 전부 통과**.
- **커버리지(JaCoCo LINE)**: covered 564 / missed 66 → **89.5%** (80% floor 통과).
- **통합 테스트 4건 미실행**: EnrollmentConcurrencyIntegrationTest·EnrollmentIntegrationTest + 기존 Meeting/Auth. 원인: Windows/Rancher Desktop docker-java JNA 초기화 실패(`DockerClientProviderStrategy`). **코드 결함 아님**(Bolt 1/2 동일). 아래 라이브 E2E로 대체 실증.

## 프론트엔드 (`/frontend`)
- **빌드**: `tsc -b && vite build` → 타입 에러 0, dist 산출(1675 모듈).
- **테스트**: **82 테스트 / 16 파일 전부 통과**. coverage **line 95.31% · branch 86.03%**(enrollments.ts 100%, MeetingListPage 97.5%, MyLearningPage 92.79%).
- **Lint**: 0(기존 SurveyBuilder 경고 1, 미변경).

## 라이브 E2E — 실행 앱 + 실제 PostgreSQL (핵심 무결성 실증)

Testcontainers 미가용 대체로 실제 스택(docker-compose Postgres + bootRun, V4 적용) 기동 후 REST API로 검증. 관리자는 MENTEE 가입 후 SQL role 승격. **16/16 통과.**

| 시나리오 | 결과 |
|---|---|
| **정원 무결성: capacity=1 + 3 병렬 apply** | **정확히 1×201 + 2×409, DB APPLIED count=1 (overbooking 금지)** ✅ |
| 중복 신청(동일 멘티 2회) | 409 ENROLLMENT_DUPLICATE ✅ |
| 취소 → 빈자리 복귀 → 타 멘티 재신청 | 204 → 201 ✅ |
| 비멘티(멘토) 신청 | 403 ✅ |
| 소유 멘토 listApplicants / 타 멘티 listApplicants | 200 / 403 ✅ |
| 멘티 listMyEnrollments | 200(본인 신청 반환) ✅ |
| ②시작(IN_PROGRESS) 후 취소 | 409 ENROLLMENT_CANCEL_FORBIDDEN ✅ |
| PENDING_APPROVAL 모임 신청 | 409 ENROLLMENT_NOT_OPEN ✅ |

- **핵심 가설 실증**: 잔여 1석 동시 신청·중복 경계가 실제 DB에서 정원 무결성을 지킴(어드바이저리 락 + count + unique).

## UI E2E (브라우저, 3역할 탭, Playwright)

프론트 dev(:5177) + 백엔드(CORS dev origin) + 실제 Postgres. 멘토/멘티A/멘티B/관리자를 각각 별도 탭에 로그인(세션 탭별 격리):

- **멘토**: 모임 개설(정원 2, #14). 운영 허브("내 러닝")에서 신청자 목록 표시(Bolt 2 placeholder 대체) — 신청 전 → 후 실시간 반영.
- **멘티A**: 모집중 카드 "신청" 버튼 → "신청완료"·"신청이 완료되었습니다". "내 러닝"에 신청 현황(신청됨·신청일·취소 버튼) 표시.
- **멘티B**: 같은 모임 신청 성공(2번째 좌석).
- **크로스탭**: 멘토 허브 "신청자: 2명 / 정원 2명"(menteeA·menteeB) → 멘티A "신청 취소"(→취소됨) 후 멘토 허브 "신청자: 1명 / 정원 2명"(menteeB만) 갱신 확인.
- (관리자 개설 승인은 이번 세션에서 API로 셋업 — 관리자 승인 UI는 Bolt 2에서 이미 검증. FE 숫자 입력 조회에 경미한 상태갱신 이슈 관측 → 후속 개선 후보.)
- **결과**: 멘티 신청/취소·정원 반영·멘토 신청자 목록이 UI에서 3역할 걸쳐 정상 동작.

## 실패·조치
- 코드 결함 실패 0. 통합 테스트 미실행은 환경 원인 — 존치, 라이브 E2E로 보완.

## 알려진 제한
- 통합 테스트는 Docker 접근 가능 환경 필요. deployment-ready 아님(CI/CD·operation은 스코프 SKIP).
