# Integration Test Instructions — apply-button-state

```
cd backend && ./gradlew test --tests "com.learnkk.integration.*"
```
- **Docker(Testcontainers) 필요** — Postgres 컨테이너 기동.
- 현재 환경에서는 Testcontainers Docker 클라이언트 초기화 실패(`DockerClientProviderStrategy`)로 21개 통합테스트가 실행 불가. 이는 환경 제약이며 코드 로직과 무관.
- 관련 시나리오: `EnrollmentIntegrationTest`(신청/정원/취소/재신청), `MeetingIntegrationTest`(모집 목록). 정상 Docker 환경에서 재실행 권장.
- 수동 확인(로컬 서버): 멘티로 로그인 → 모집 목록에서 (1) 이미 신청한 모임 "신청완료" 비활성, (2) 정원 찬 모임 "마감" 배지+비활성 확인.
