# Team Practices — learnKK (런크크)

<!-- practices-discovery 최종본(FINAL). 리드(pipeline-deploy-agent) 초안 → 지원 3인(quality/developer/devsecops) blind review → 인터뷰(Q1~Q6 전부 A 확정)를 통합했다. 이번 워크플로우는 설계 전용이라 아래 practice는 실행되지 않고 team.md로 승격되어 팀 3인의 개별 구현 워크플로우가 상속하는 계약이다. team voice로 작성. -->

이 문서는 practices-discovery **최종본**이다. org.md 기본값을 출발점으로, learnKK의 확정 제약(전부 로컬, React+Spring+PostgreSQL, 이번 워크플로우는 설계까지)에 맞춰 특화하고, 인터뷰에서 팀이 확정(Q1~Q6 전부 A)한 결정을 반영했다. 여기 확정된 다섯 섹션 + 보안 자세는 team.md로 승격되어 팀 3인의 개별 구현 워크플로우가 상속한다.

## Way of Working

우리는 **trunk-based development**를 기본으로 한다. 모든 작업은 짧게 사는(1~2일) feature branch로 진행하고 `main`에 병합한다. 장기 브랜치는 병합 부채를 쌓으므로 피한다. Bolt 브랜치는 **squash-merge**하여 `main`을 선형으로 유지한다(org 기본값 그대로).

저장소는 **monorepo**로 둔다: `/frontend`(React), `/backend`(Spring), `/contracts`(공유 계약). 3인 전원 풀스택·기능 수직 슬라이스·독립 병렬 구조에서, FE와 BE를 한 repo에 두면 공유 계약(API 스펙·DTO/타입·DB 스키마)을 한 소스에서 원자적으로 버전 관리할 수 있어 인터페이스 drift를 줄인다.

이 팀의 지배적 리스크는 **단위 간 interface 불일치**다(team-assessment 기준). 이를 통제하기 위해 공통 기반(인증·모임 도메인·DB 스키마)을 **구현 전 3개 계약 아티팩트로 고정**한다:

1. **API 계약** — OpenAPI 스펙 한 소스(엔드포인트·요청/응답 DTO·상태코드·에러 스키마).
2. **DB 스키마 계약** — 초기 스키마 + 마이그레이션(컬럼명·타입·제약을 여기서 확정).
3. **도메인 타입 계약** — 공유 도메인 용어집(모임/멘토/멘티/출석/수료 상태 enum 등)을 FE·BE가 동일 이름으로 참조.

이 세 계약은 각 개발자 슬라이스보다 **먼저** 확정·소유자 배정된다. 계약의 소유·고정 순서 확정은 delivery-planning으로 이월한다.

Construction worktree의 base 브랜치·merge target은 `main`이다. 이번 워크플로우는 설계 전용이라 실제 Construction worktree/merge는 팀 3인의 개별 구현 워크플로우에서 발생하며, 그때 이 규칙을 적용한다.

## Walking Skeleton

팀 3인의 구현 워크플로우에서 **walking skeleton을 먼저 한 번 세운다.** 공통 기반(인증 + 모임 도메인 + DB 스키마)을 얇게 관통하는 슬라이스를 완성한 뒤, 각 개발자가 자기 수직 슬라이스로 갈라진다. 독립 병렬 + 계약 우선 구조에서 skeleton을 먼저 세우면 interface 불일치 리스크가 실질적으로 줄어든다.

이번 워크플로우는 설계 전용이라 이 인셉션 안에서 실제 skeleton Bolt는 실행되지 않는다. 이 결정은 team.md로 승격되어 구현 워크플로우가 상속한다.

## Testing Posture

테스트는 모든 Bolt에서 1급 산출물로 취급하며 **코드와 함께 작성**한다(test-alongside). 병합 전 "테스트 통과 + coverage floor 충족"을 게이트 규칙으로 team.md에 상속 계약으로 고정한다. 실행 플랫폼(GitHub Actions 등)은 미정으로 두되, 게이트의 pass/fail **의미**는 지금 확정한다.

프레임워크(스택 확정):

- **백엔드(Java Spring)** — JUnit 5(Jupiter) + Spring Boot Test, 웹 계층 **MockMvc**(`@WebMvcTest`), 서비스 계층 격리 **Mockito**.
- **프론트엔드(React/TS)** — **React Testing Library + Vitest**(Vite 기반 가정; 빌드 도구가 webpack/CRA로 확정되면 Jest로 대체). 사용자 상호작용은 `@testing-library/user-event`.
- **통합 테스트** — **Testcontainers 로컬 PostgreSQL.** 전부-로컬 제약(외부 SaaS·클라우드 금지)을 위반하지 않는 로컬 Docker 도구이며, 재현성·독립성을 확보한다. 안티-중복계정 로직 등 PostgreSQL 고유 기능 의존 부분은 실 PostgreSQL(Testcontainers)로 검증한다. Deployment의 docker-compose 표준화와 묶어 운용한다.
- **API 계약 테스트 계층** — 프론트-백엔드 interface 불일치가 최대 리스크이므로, 단위/통합과 별도로 **OpenAPI 스펙 기반 API 계약 검증**(응답 스키마 고정)을 posture에 명시한다.

Coverage: **80% line coverage floor**를 유지하되 **프론트/백엔드 각각** 측정한다(백엔드 JaCoCo, 프론트 Vitest `--coverage`). line coverage만으로는 도메인 규칙 분기(관리자 4지점 승인, 출석률 80% 자동 수료 판정 등)를 놓치므로, 이 규칙 로직은 **branch/시나리오 커버리지**로 별도 보강한다. coverage는 목표가 아니라 가이드다. acceptance criteria를 직접 검증하는 테스트를 작성한다(shift-left).

이번 설계 워크플로우 자체는 코드가 없어 테스트 실행은 없고, 이 posture는 team.md에 확정되어 구현 워크플로우가 상속한다.

## Deployment

**로컬, 설계 전용.** 이번 워크플로우에서 배포는 범위 밖이다(constraint-register T3/T5, scope-document out-of-scope). org.md의 클라우드 CD 기본값("deploy on merge to staging / 프로덕션 수동 승인")은 이 워크플로우에 **적용하지 않는다.**

로컬 실행 모델은 **docker-compose로 표준화**한다(PostgreSQL 등 의존 서비스 기동). 이는 통합 테스트의 Testcontainers 결정과 정합적이다. 시크릿(DB 자격증명·세션 시크릿 등)은 **repo에 커밋하지 않고**(`.env`류 gitignore) 환경변수·Spring profile(`application-local.properties`)로 주입하며, `.env.example` 예시 파일을 둔다.

CI/CD 파이프라인 설계·구축은 팀 3인의 **후속 구현 워크플로우로 이월**한다.

## Code Style

린터/포매터(스택 확정):

- **프론트엔드(React/TS)** — **Prettier + ESLint**(+`@typescript-eslint`, TypeScript 사용). repo 루트 `.prettierrc`/`eslint.config.js`.
- **백엔드(Java Spring)** — **Spotless + google-java-format**(포매터) + **Checkstyle**(규칙 린터). Gradle/Maven 플러그인으로 build 연결.
- 규약 설정은 monorepo 루트에 두어 3인이 동일 포맷으로 커밋한다(diff 노이즈·스타일 충돌 방지). 실제 설정 파일 생성은 구현 워크플로우로 이월.

네이밍:

- FE(JS/TS): 변수·함수·prop `camelCase`, 컴포넌트·타입·인터페이스 `PascalCase`, 상수 `UPPER_SNAKE`.
- BE(Java): 클래스 `PascalCase`, 메서드/필드 `camelCase`, 상수 `UPPER_SNAKE`, 패키지 lowercase.
- DB(PostgreSQL): 테이블·컬럼 `snake_case`.

경계 규약(병렬 리스크 직결):

- **JSON API 필드는 `camelCase`로 통일**(Jackson `PropertyNamingStrategy` 명시), **JPA는 물리 네이밍 전략을 `snake_case`로** 일괄 처리한다. FE가 필드 케이스를 추측하지 않게 해 인터페이스 불일치를 막는다.
- BE는 `Controller → Service → Repository` 3계층을 두고 **Entity를 API 경계 밖으로 노출하지 않는다**(Controller는 Request/Response DTO만). FE는 API 호출을 **단일 API client 계층**으로 모아 BE 계약의 소비 지점을 한 곳으로 둔다.
- 전역 에러 응답 스키마를 **커스텀 `{code, message, details}`** 하나로 고정한다(Spring `@RestControllerAdvice` 전역 처리). HTTP 상태코드 규약: 검증 실패 400, 인증 401, 인가 403, 미존재 404. 예외를 삼키지 않는다(fail loud). 사용자 노출 메시지는 한글(project.md 규약).

## Security Posture (로컬 한정, 구현 워크플로우 상속)

스타일 린터와 별개로 **보안 계층**을 team.md 자세로 확정한다. 전부 오프라인 로컬 실행이며 외부 SaaS·클라우드 보안 서비스로 확장하지 않는다(T3 준수).

- **비밀번호 해시** — 적응형 해시 **bcrypt**(Spring Security `PasswordEncoder` 기본)로 저장. 평문·가역 암호 저장 금지.
- **시크릿 취급** — 비밀값을 repo에 커밋하지 않는 것을 hard rule(NEVER)로 둔다(`.env`류 gitignore + `.env.example` 제공, 환경변수/Spring profile 주입).
- **보안 정적분석** — 병합 전 게이트로 BE **SpotBugs + FindSecBugs**(SQL injection·약한 암호·하드코딩 시크릿 탐지), FE **eslint-plugin-security**(XSS·`dangerouslySetInnerHTML` 오용 탐지). 전부 로컬.
- **의존성 스캔** — FE `npm audit` + `package-lock.json` 커밋·버전 핀, BE **OWASP Dependency-Check**(NVD 로컬 캐시로 오프라인) + 의존성 락(Gradle `dependencyLocking` 또는 Maven 버전 고정).
- **안티-중복계정 신호 처리** — 원본 IP 등을 그대로 저장하지 않고 **salted hash 또는 부분 마스킹**으로 저장, 명시적 보관 창(TTL) 후 파기, UI·API·로그·에러 어디에도 비노출, 접근은 안티-중복 판정 로직으로 한정. 이 데이터 플로우·보관 정책의 상세는 **nfr-requirements / functional-design로 이월**한다.

## Assumptions & Open Questions

- 다섯 섹션 + 보안 자세는 인터뷰(Q1~Q6 전부 A)로 확정되어 team.md 승격 대상이다.
- 이번 워크플로우는 설계 전용이라 Walking Skeleton·Deployment·Testing·보안 스캔 실행은 이 인셉션 안에서 일어나지 않고, 팀 3인의 개별 구현 워크플로우가 상속한다.
- 공유 계약(API/DB/도메인 타입)의 소유·고정 순서, DB 마이그레이션 도구(Flyway/Liquibase), BE 빌드(Gradle vs Maven·모듈 구성)는 delivery-planning/구현 워크플로우에서 확정한다.
- 개발자 3인의 구체 식별·proto-Unit 매핑은 units-generation·delivery-planning에서 확정한다.
- 안티-중복계정 신호의 보관 형태·보관 창·비노출 경계 상세는 nfr-requirements/functional-design로 이월한다.
