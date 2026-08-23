# Build & Test Summary — Bolt 5 Messaging (learnKK)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). Test Strategy=Standard. 상류: bolt5-messaging code-generation-plan.md · code-summary.md. -->

## 전체 빌드 상태·전제

- 백엔드·프론트엔드 모두 컴파일·정적검사·실행 가능 테스트 통과(실측: build-test-results.md). 전제: Java 21, Node/npm, Docker(통합/로컬 DB). Flyway V1~V5. 시크릿 `.env` 비커밋.

## 테스트 유형 인벤토리 (Standard)

| 유형 | 지시서 | 상태 |
|---|---|---|
| 단위 | unit-test-instructions.md | 생성·실행(send 경계 403/400/404·getThread·unread·listThreads·listRecipients) |
| 통합 | integration-test-instructions.md | 생성(경계 관통·스레드 유일성·멱등 확인). Docker 필요로 이 환경 미실행 → web/service 테스트로 대체 실증 |
| API 계약 | (contract) | OpenApiContractTest 통과(messaging 스키마 4종 추가) |
| 보안 | security-test-instructions.md | 생성(인가 경계·입력검증·401/403/400) |
| 성능 | performance-test-instructions.md | 경량 가이드(파일럿, 정식 부하 이월) |

## 커버리지 기대 대비 실측

- **백엔드 LINE 86.6%**(covered 678 / 783), messaging 패키지 포함. `jacocoTestCoverageVerification`(≥80% floor) 통과.
- **프론트 전체 94.87%**(messaging 91.9%, messages.ts 100%, AppShell 100%, ThreadView 94.11%, MessagesPage 88.6%). 각 ≥80% floor.

## 준비도 평가

- **build-ready**: 예. **test-ready**: 예(BE 단위·웹·계약 + FE 91 테스트 통과, 권한 경계 403/401/400 로컬 실증). 통합 테스트는 Docker 환경 필요.
- **deployment-ready**: 아니오 — ci-pipeline·operation은 project.md Scope Override로 SKIP. 로컬 실행만.

## 알려진 제한·미결 항목

- **환경**: Testcontainers 통합(MessageIntegrationTest) 미실행(Windows/Rancher JNA, Bolt 1~3 동일). 코드 결함 아님 — 권한 경계는 MockMvc 전체 체인(interceptor→advice) 테스트로 실증.
- **이월(Bolt 6+)**: 실시간 푸시/웹소켓(현재 폴링), 첨부·그룹 쪽지, U9 알림 조합, 정식 부하 벤치.
