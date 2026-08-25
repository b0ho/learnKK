# Integration Test Instructions — ux-bugfixes-2

백엔드 통합테스트(`*IntegrationTest`)는 Testcontainers로 실제 PostgreSQL을 띄운다.

```bash
cd backend
./gradlew test    # Docker 데몬 필요
# 비표준 소켓(예: Rancher/Colima):
DOCKER_HOST=unix://$HOME/.rd/docker.sock DOCKER_API_VERSION=1.43 TESTCONTAINERS_RYUK_DISABLED=true ./gradlew test
```

- 이번 실행 환경에서는 Docker/Testcontainers 초기화 제약(DockerClientProviderStrategy)으로 21개 `*IntegrationTest`가 실패했다. 코드 결함이 아니라 환경 제약이며 이전 Bolt들과 동일하다.
- 신규 계약 변경(AttendanceSummaryResponse.attendedSessionIds)은 `OpenApiContractTest`(단위)로 검증된다.
- FR-11 시드(V12)는 Flyway로 부팅 시 적용되며, 로컬 Postgres + bootRun으로 수동 검증 가능(관리자 대시보드/내 러닝/승인 큐에서 다양한 상태 확인).
