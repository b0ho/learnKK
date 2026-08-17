# Evidence — Practices Discovery (learnKK)

<!-- practices-discovery 리드 초안(DRAFT). 리드가 무엇을 검토·추론했는지, greenfield라 코드/CI 증거가 없다는 점, 인터뷰가 풀어야 할 미해결 불확실성을 기록한다. 지원 에이전트(quality/developer/devsecops) 기여와 인터뷰 결정은 최종 통합에서 보강된다. -->

## Inspected & Inferred

리드(pipeline-deploy-agent)가 초안 작성을 위해 검토·추론한 근거:

- **org.md 기본값 (5개 섹션)** — Way of Working(trunk-based, 1~2일 feature branch, squash-merge), Walking Skeleton(scope `skeleton:` 선언 기반), Testing Posture(scope별 기본, feature/mvp는 80% coverage), Deployment(deploy on merge to staging + 프로덕션 수동 승인), Code Style(프로젝트 린터/포매터 위임). 이들은 **제안 기본값**으로 취급했고 팀 확정 사실이 아니다.
- **project.md locks** — Tech Stack: React + Java Spring + PostgreSQL 전부 로컬(feasibility 확정). Decided: 비밀번호 해시 저장 + 히든 안티-중복계정 장치, 관리자 4지점 승인, 출석율 80% 자동 수료 판정. Corrections: 사용자 산출물 한글 작성 규약, §13 추천 항목 최상위 명시, 승인 게이트 시 관련 변경만 staged.
- **constraint-register (feasibility)** — T1 React, T2 PostgreSQL, T3 전부 로컬·외부 SaaS 미사용, T4 모바일 웹뷰 우선, T5 이번은 설계까지. R1 개인정보 최소·해시, R2 특정 규제 해당 없음, R3 안티-중복계정 신호 목적 한정·최소보관·비노출.
- **initiative-brief** — 판정 Go, 이번 워크플로우는 설계까지, 구현·배포는 팀 3인의 개별 워크플로우로 분리.
- **team-assessment** — 개발자 3인 전원 풀스택, 기능 수직 슬라이스 분배, 독립 병렬 협업, 핵심 리스크는 단위 간 interface 불일치 → 공통 기반 계약 우선.

## Greenfield Evidence Gap

이 프로젝트는 **greenfield**다. 아직 코드베이스·git 히스토리·CI 설정·배포 구성이 없어 브라운필드식 증거(branching 실측, CI 파이프라인, 코드 관례 추론)는 **존재하지 않는다.** 따라서 team-practices 초안은 실측 증거가 아니라 org.md 기본값 + 확정 제약을 특화한 **제안**이며, 팀 의도는 인터뷰에서 사람이 확정해야 한다. upstream-coverage 센서의 브라운필드 조건부 입력(code-structure, technology-stack 등)은 설계상 부재한다.

## Support Agent Blind Review (folded in)

지원 3인의 독립 검토가 리드 초안을 보강했고, 최종 통합에 반영됐다:

- **quality-agent** — 프레임워크 조합(JUnit5+Spring Boot Test+MockMvc+Mockito / RTL+Vitest) 지지. 핵심 갭으로 **API 계약 테스트 계층 부재**와 **통합 테스트 DB 재현성**을 지적 → Testcontainers 로컬 PostgreSQL + OpenAPI 기반 계약 테스트 계층을 posture에 추가. coverage floor는 FE/BE 각각 측정 + 도메인 규칙 branch/시나리오 보강.
- **developer-agent** — 최대 리스크(독립 병렬 interface 불일치)를 코드-레벨 계약으로 통제할 것을 강조 → **monorepo(`/frontend`,`/backend`,`/contracts`)** + 3개 계약 아티팩트(OpenAPI/DB 스키마/도메인 타입) + 경계 규약(JSON camelCase↔JPA snake_case, Entity 비노출, 커스텀 에러 스키마)을 확정. 네이밍·에러·Entity-비노출을 project.md 승격 후보로 제시.
- **devsecops-agent** — 스타일 린터가 커버하지 않는 **보안 계층**을 분리 명시 → bcrypt 해시, 시크릿 비커밋 hard rule, 보안 정적분석(SpotBugs+FindSecBugs / eslint-plugin-security), 의존성 스캔(npm audit / OWASP Dependency-Check), 안티-중복계정 신호 보관·비노출 규약을 확정·이월.

## Interview Resolution (Q1~Q6 = A)

인터뷰에서 팀이 위 open uncertainty를 전부 권장 번들(A)로 확정했다:

- **Q1 Way of Working** = A — trunk-based+squash+monorepo + 3개 계약 우선 고정.
- **Q2 Walking Skeleton** = A — 공통 기반 관통 skeleton을 먼저 한 번.
- **Q3 Testing Posture** = A — JUnit5/MockMvc/Mockito + RTL/Vitest + Testcontainers + 80% floor(각각) + test-alongside + OpenAPI 계약 테스트.
- **Q4 Deployment** = A — docker-compose 표준화 + 시크릿 비커밋(`.env`/`.env.example`) + CI/CD 이월.
- **Q5 Code Style** = A — Prettier+ESLint(+TS) / Spotless+google-java-format+Checkstyle + 경계 규약(camelCase/snake_case, Entity 비노출, 커스텀 에러 스키마).
- **Q6 Security** = A — bcrypt + 시크릿 비커밋 NEVER + 보안 정적분석 + 의존성 스캔 + 안티-중복 신호 이월.

승격 결과: 계약 우선·네이밍/에러 경계·시크릿 비커밋·Entity 비노출·bcrypt는 discovered-rules의 `[affirmed]` hard rule로, 나머지 practice 자세는 team-practices로 확정됐다.

## Assumptions & Open Questions

- 인터뷰로 해소되지 않고 명시적으로 하류로 이월된 항목: 계약 소유·고정 순서(delivery-planning), DB 마이그레이션 도구·BE 빌드 구성(구현 워크플로우), 안티-중복계정 신호 보관 상세(nfr-requirements/functional-design).
- 프론트 빌드 도구가 webpack/CRA로 확정되면 Vitest→Jest로 러너만 대체(RTL은 불변).
