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

## Open Uncertainties (인터뷰가 해결해야 할 것)

- **Way of Working** — trunk-based/squash 정책을 팀이 그대로 채택할지. 공통 기반 interface contract의 소유·고정 순서(delivery-planning 연계).
- **Walking Skeleton** — 팀 구현 워크플로우에서 skeleton을 켤지. 공통 기반을 관통하는 skeleton을 먼저 세울지.
- **Testing Posture** — 테스트 프레임워크(JUnit/MockMvc, Vitest·Jest+RTL) 확정, coverage floor 80% 유지/조정, 방법론(TDD/BDD/test-after).
- **Deployment** — 로컬 실행/기동 표준화 필요 여부(예: docker-compose로 PostgreSQL 기동). CI/CD 설계를 어느 구현 워크플로우로 이월할지.
- **Code Style** — 구체 린터/포매터 도구 선택과 설정, 린터 위에 얹을 추가 팀 관례(named export, 예외 처리 규약, 레이어 경계).

## Assumptions & Open Questions

- 지원 에이전트(quality/developer/devsecops)의 blind review 기여와 인터뷰 결정은 이 초안에 아직 반영되지 않았고, 리드 최종 통합에서 보강된다.
- 위 open uncertainties는 인터뷰 질문의 근거이며, 해소된 답만 team.md/project.md로 승격된다.
