# Build & Test Summary — Bolt 1 Walking Skeleton (learnKK)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). 출처: code-generation-plan.md·code-summary.md(생성 코드/스택), team.md Testing Posture, U2/U3 nfr-requirements. Test Strategy=Standard. -->

## 전체 빌드 상태·전제

- 백엔드(Spring Boot/Java 21)·프론트엔드(React+TS+Vite) 모두 **빌드·테스트 통과**(실측: build-test-results.md).
- 전제: Java 21, Node/npm, Docker(통합 테스트). 시크릿은 `.env` 주입(커밋 금지).

## 테스트 유형 인벤토리 (Standard)

| 유형 | 지시서 | 상태 |
|------|--------|------|
| 단위 | unit-test-instructions.md | 생성·실행(BE 69·FE 59 포함) |
| 통합 | integration-test-instructions.md | 생성·실행(Testcontainers 관통 실증) |
| API 계약 | (integration에 포함) | OpenApiContractTest 통과 |
| 보안 | security-test-instructions.md | 생성(인증/인가 단위·통합으로 검증) |
| 성능 | performance-test-instructions.md | 가이드(파일럿 규모, 정식 부하는 이월) |

## 커버리지 기대 대비 실측

- 백엔드 LINE 89.8% (≥80% floor). 프론트 LINE 95.22% (≥80% floor). 도메인 분기(①승인·409·403·401) 시나리오 커버.

## 준비도 평가

- **build-ready**: 예(양 스택 클린 빌드).
- **test-ready**: 예(단위·통합·계약 통과, floor 충족).
- **deployment-ready**: 아니오 — CI/CD·배포·operation은 이번 스코프에서 SKIP(project.md Scope Overrides). 로컬 실행(docker-compose + bootRun + vite dev)만 지원.

## 알려진 제한·미결 항목

- Bolt 1 최소 슬라이스: U3 ②/③/모집확정·문항 게이팅·운영 허브 read 조합, U4~U9 미구현(Bolt 2+ 이월).
- 리뷰어 지적 follow-up(non-blocking): kernel→auth 순환 의존(WebConfig) — Bolt 2 착수 전 해소 권고(code-generation memory.md 기록).
- 통합 테스트는 Docker 필요(Rancher Desktop 환경변수 문서화).
