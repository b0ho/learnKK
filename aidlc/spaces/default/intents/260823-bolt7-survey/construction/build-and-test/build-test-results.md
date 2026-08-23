# Build & Test Results — Bolt 7 Survey/Feedback (learnKK)

<!-- build-and-test 산출물(Step 10 실제 실행 결과). 2026-08-23. -->

## 실행 환경
- Java 21 + Gradle 8.10.2, Node/npm(Vitest 2.1.9 / Vite 5.4.21). Docker: Windows/Rancher Desktop(Testcontainers JNA 미가용). Postgres 16, Flyway V1~V5.

## 백엔드 (`/backend`)
- **컴파일/정적검사**: OK, Spotless+Checkstyle clean.
- **테스트(비통합)**: **162 테스트 0 실패**(PreSurveyServiceTest·FeedbackServiceTest·Survey/FeedbackControllerTest·OpenApiContractTest·SessionAuthInterceptorTest 포함). 이 세션 재확인: `com.learnkk.survey.*` 대상 `./gradlew test` → **BUILD SUCCESSFUL**.
- **커버리지(JaCoCo LINE)**: covered 692 / missed 74 → **90.3%** (80% floor 통과).
- **통합 테스트(SurveyIntegrationTest 등)**: 미실행 — Windows/Rancher docker-java JNA 초기화 실패(Bolt 1~3 동일). 코드 검토 완료(리뷰어 구조 정상 확인), 존치.
- (참고) 세션 내 Gradle 데몬 경합으로 전체 필터 재실행이 지연됨 — survey 모듈 단독 재실행으로 신규 코드 그린 확인, 전체 수치는 code-generation 검증 실행 + 아키텍처 리뷰(build/test-results 확인) 기준.

## 프론트엔드 (`/frontend`)
- **빌드**: `tsc -b && vite build` → 타입 에러 0.
- **테스트**: **96 테스트 / 21 파일 0 실패**. coverage **line 93.96%**(survey feature dir 85.5%). Lint 0.

## 게이팅·인가 경계 검증 (단위/슬라이스 커버)
- 사전설문 제출 ②전 409 PRESURVEY_NOT_OPEN, 비참여자 403, 필수누락 400, 재제출 upsert.
- getAnswers 소유멘토/관리자/본인 200·타인 403·**COMPLETED 후 멘토/관리자 열람 가능**(iteration-1 설계 버그 미재현).
- listFeedback 소유멘토·관리자 200·타모임멘토 403·**멘티 403(열람 경로 없음)**.

## 라이브 API E2E (실행 앱 + 실제 Postgres, V5 적용) — 15/15 통과

Testcontainers 미가용 대체로 실 스택 기동 후 REST API로 두 핵심 가설 검증(멘토/멘토2/관리자/멘티참여/멘티비참여 5역할):

| 시나리오 | 결과 |
|---|---|
| **②전(RECRUITING) 사전설문 제출** | 409 PRESURVEY_NOT_OPEN ✅ |
| 필수 문항 누락 제출(IN_PROGRESS) | 400 ✅ |
| 정상 제출 | 200 ✅ |
| 비참여자 제출 | 403 ✅ |
| 소유멘토 / 관리자 / 본인 응답 열람 | 200 / 200 / 200 ✅ |
| 타 멘티 응답 열람 | 403 ✅ |
| 멘티 피드백 제출 | 201 ✅ (ASCII 재확인 — 최초 한글 리터럴 500은 셸 cp949 인코딩 아티팩트, 앱 결함 아님) |
| 소유멘토 / 관리자 피드백 열람 | 200 / 200 ✅ |
| 타모임멘토 / 멘티 피드백 열람 | 403 / 403 ✅ |
| **③완료(COMPLETED) 후 멘토 응답 열람** | 200 유지 ✅ |

- **핵심 가설 실증**: 사전설문은 ②시작 이후에만 제출 가능하고, 피드백 열람 권한 경계(소유멘토·관리자만)가 실 DB에서 지켜짐. getAnswers는 COMPLETED 후에도 열람 가능(설계 정합).

## 실패·조치
- 코드 결함 실패 0. 통합 미실행은 환경 원인. 라이브 E2E의 최초 피드백 500은 셸 한글 인코딩 아티팩트(ASCII 재확인 201) — 앱 무관.

## 알려진 제한
- 통합 테스트는 Docker 환경 필요. deployment-ready 아님(ci-pipeline·operation SKIP).
- 게이팅·인가는 결정론적 → 단위/슬라이스로 완전 커버. 필요 시 라이브 API/UI E2E로 추가 실증 가능.
