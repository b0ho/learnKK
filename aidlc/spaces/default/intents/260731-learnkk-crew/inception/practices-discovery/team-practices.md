# Team Practices — learnKK (런크크)

<!-- practices-discovery 리드(pipeline-deploy-agent) 초안(DRAFT). org.md 기본값을 이 프로젝트(로컬 React + Java Spring + PostgreSQL, 이번은 설계 전용)에 맞춰 특화했다. `[확인 필요]` 표시는 인터뷰에서 사람이 확정해야 하는 제안이다. team voice로 작성. -->

이 문서는 practices-discovery **초안**이다. 아래 다섯 섹션은 org.md 기본값을 출발점으로, learnKK의 확정 제약(전부 로컬, React+Spring+PostgreSQL, 이번 워크플로우는 설계까지)에 맞춰 특화한 제안이다. `[확인 필요]`로 표시된 항목은 인터뷰에서 팀이 확정해야 반영된다.

## Way of Working

우리는 **trunk-based development**를 기본으로 한다. 모든 작업은 짧게 사는(1~2일) feature branch로 진행하고 `main`에 병합한다. 장기 브랜치는 병합 부채를 쌓으므로 피한다. 이는 org.md 기본값 그대로이며, 이 팀 구성에 잘 맞는다.

이 프로젝트의 팀 구성은 개발자 3인 전원 풀스택(React+Spring+PostgreSQL), **기능 수직 슬라이스** 분배, **독립 병렬** 협업이다(team-assessment 기준). 각 개발자는 자기 proto-Unit 묶음을 프론트~백엔드까지 통째로, 별도의 개별 구현 워크플로우로 진행한다. 독립 병렬의 핵심 리스크는 **단위 간 interface 불일치**이므로, 공통 기반(인증·모임 도메인·DB 스키마)을 interface contract로 먼저 고정하는 것을 way of working의 전제로 둔다. `[확인 필요]` — 계약 고정의 소유·순서는 delivery-planning에서 확정.

Construction worktree의 base 브랜치와 merge target은 `main`이다(org 기본값). Bolt 브랜치는 **squash-merge**하여 `main`을 선형으로 유지한다. `[확인 필요]` — 이번 워크플로우는 설계 전용이라 실제 Construction worktree/merge는 팀 3인의 개별 구현 워크플로우에서 발생하며, 그때 이 규칙을 적용한다.

## Walking Skeleton

Walking-skeleton 여부는 활성 scope 파일의 `skeleton:` 선언을 따른다(org 기본값). 이번 워크플로우는 **설계 전용**이라 이 인셉션 안에서 실제 skeleton Bolt는 실행되지 않는다.

`[확인 필요]` — 팀 3인의 개별 구현 워크플로우에서 skeleton을 켤지 여부. 독립 병렬 + interface 계약 우선이라는 특성상, 공통 기반(인증·모임 도메인·DB 스키마)을 얇게 관통하는 walking skeleton을 **먼저 한 번 세우고** 각자 수직 슬라이스로 갈라지는 방식이 리스크(인터페이스 불일치)를 줄이는 유력한 후보다. 이는 제안이며 팀이 인터뷰에서 확정한다.

## Testing Posture

테스트는 모든 Bolt에서 1급 산출물로 취급한다(org 기본값). scope별 기본값에 따라 `feature`/`mvp` 계열은 코드와 함께 테스트 작성, 최소 80% line coverage, 병합 전 테스트 통과를 기본으로 둔다.

스택에 맞춘 특화 제안: 백엔드는 **JUnit**(+ Spring Boot Test / MockMvc), 프론트엔드는 **Vitest/Jest + React Testing Library**, 통합 테스트는 로컬 PostgreSQL을 대상으로 한다. `[확인 필요]` — 정확한 테스트 프레임워크 선택, coverage floor(80% 유지/조정 여부), TDD/BDD/test-after 중 어떤 방법론을 쓸지는 인터뷰에서 확정. 이번 설계 워크플로우 자체는 코드가 없으므로 테스트 실행은 없고, 이 posture는 team.md에 확정되어 이후 구현 워크플로우가 상속한다.

## Deployment

**로컬, 설계 전용.** 이번 워크플로우에서 배포는 범위 밖이다. constraint-register가 고정한 대로 전부 로컬 환경이고 외부 SaaS/클라우드/CI 플랫폼을 쓰지 않으며, 배포·CI·운영 인프라는 scope-document에서 명시적으로 out-of-scope다.

따라서 org.md의 "deploy on merge to staging / 프로덕션 수동 승인" 클라우드 CD 기본값은 **이 워크플로우에는 적용하지 않는다.** 로컬 실행 모델은 개발자 머신에서 React dev server + Spring Boot(내장 톰캣) + 로컬 PostgreSQL 인스턴스를 띄우는 수준으로 본다. CI/CD 파이프라인 설계·구축은 팀 3인의 **별도 구현 워크플로우로 이월**한다. `[확인 필요]` — 로컬 실행/기동 방식(예: docker-compose로 PostgreSQL 기동 여부)의 표준화가 필요한지는 인터뷰에서 확인.

## Code Style

프로젝트 레벨 설정에 위임한다(org 기본값). 스택에 맞춘 제안: 프론트엔드(React/TS)는 **Prettier + ESLint**, 백엔드(Java Spring)는 **google-java-format 또는 Spotless + Checkstyle** 급의 포매터/린터를 repo 루트 설정으로 둔다. 네이밍은 언어 관용을 따른다 — JS/TS는 camelCase, Java는 표준 Java 관례(클래스 PascalCase, 메서드/필드 camelCase), PostgreSQL 식별자는 snake_case.

`[확인 필요]` — 구체 포매터/린터 도구 선택과 설정, 그리고 팀이 린터 위에 얹고 싶은 추가 관례(예: named export 선호, 예외 처리 규약, 레이어 경계 규칙)는 인터뷰에서 확정. 프레임워크의 코드 스타일 제안은 프로젝트 린터 설정이 이미 커버하지 않을 때만 발화한다.

## Assumptions & Open Questions

- 다섯 섹션 모두 org.md 기본값을 특화한 **제안**이며, `[확인 필요]` 항목은 인터뷰에서 팀이 확정해야 team.md로 승격된다.
- 이번 워크플로우는 설계 전용이라 Walking Skeleton·Deployment·Testing 실행은 이 인셉션 안에서 일어나지 않고, 팀 3인의 개별 구현 워크플로우가 상속한다.
- 개발자 3인의 구체 식별·proto-Unit 매핑은 units-generation·delivery-planning에서 확정(team-assessment 기준 미정).
