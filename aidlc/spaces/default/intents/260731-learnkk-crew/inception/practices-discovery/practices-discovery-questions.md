# Practices Discovery — 인터뷰 질문지 (learnKK / 런크크)

greenfield이므로 team.md 5개 섹션(Way of Working, Walking Skeleton, Testing Posture, Deployment, Code Style)을 확정합니다. 각 문항 최상단 **A. (권장)** 은 리드+3인 리뷰어(pipeline/quality/developer/devsecops)가 합의한 기본값 번들입니다. 이번은 **설계 전용** 워크플로우라, 여기서 확정된 practice는 실행되지 않고 team.md에 고정되어 팀 3인의 개별 구현 워크플로우가 상속합니다.

각 `[Answer]:` 뒤에 보기 문자를 적어주세요. 부분 수정은 `X. 기타`로 자유 서술하시면 됩니다.

---

## Q1. Way of Working — 브랜칭 + 저장소 구조 + 공유 계약
- A. (권장) trunk-based + feature branch(1~2일) + Bolt squash-merge to `main`(org 기본값 그대로) **＋ monorepo**(`/frontend`, `/backend`, `/contracts`) **＋** 공유 기반(인증·모임 도메인·DB 스키마)을 **구현 전 3개 계약 아티팩트로 고정**: ① OpenAPI 스펙 ② DB 스키마+마이그레이션 ③ 도메인 타입/enum 용어집
- B. trunk-based/squash는 채택하되 저장소는 multi-repo(프론트/백 분리)
- C. 계약 우선 고정 없이 각자 병렬 구현(계약은 delivery-planning에서만)
- X. 기타 (직접 서술)

[Answer]:a

## Q2. Walking Skeleton — 공통 기반 관통 슬라이스
- A. (권장) 팀 구현 워크플로우에서 **skeleton을 먼저 한 번 세운다** — 공통 기반(인증 + 모임 도메인 + DB 스키마)을 얇게 관통하는 슬라이스를 완성한 뒤 각자 수직 슬라이스로 분기 (독립 병렬의 interface 불일치 리스크 완화)
- B. skeleton 없이 각 개발자가 자기 슬라이스를 바로 시작
- C. 지금 결정하지 않고 delivery-planning으로 이월
- X. 기타 (직접 서술)

[Answer]:a

## Q3. Testing Posture — 프레임워크 + 커버리지 + 방법론 + 계약 테스트
- A. (권장) 백엔드 **JUnit 5 + Spring Boot Test + MockMvc + Mockito**, 프론트 **React Testing Library + Vitest**(Vite 가정; webpack/CRA면 Jest) **＋** 통합테스트는 **Testcontainers 로컬 PostgreSQL** **＋** **80% line coverage floor**(프론트/백 각각 측정, 도메인 규칙은 branch/시나리오 커버리지 보강) **＋** 방법론은 **test-alongside**(코드와 함께 작성) **＋** 프론트-백 **API 계약 테스트 계층**(OpenAPI 기반) 추가 **＋** "병합 전 테스트 통과+coverage 충족" 게이트 의미를 team.md에 상속 계약으로 고정
- B. 위 프레임워크는 채택하되 방법론을 **TDD**(테스트 먼저)로
- C. 위 프레임워크는 채택하되 방법론을 **BDD**(시나리오 주도)로
- D. coverage floor를 80%에서 조정 / Testcontainers 대신 각자 로컬 DB (자유 서술)
- X. 기타 (직접 서술)

[Answer]:a

## Q4. Deployment — 로컬 기동 + 시크릿 주입 (이번 범위=설계, 배포는 out-of-scope)
- A. (권장) 로컬 실행 모델을 **docker-compose로 표준화**(PostgreSQL 등 기동) **＋** 시크릿은 **repo에 커밋하지 않고**(`.env`류 gitignore) 환경변수/Spring profile로 주입, `.env.example` 예시 제공. CI/CD 파이프라인 설계·구축은 팀 3인의 후속 구현 워크플로우로 이월
- B. docker-compose 없이 각자 로컬 PostgreSQL 직접 설치·기동
- C. 로컬 기동 방식은 구현 워크플로우에서 정하고 지금은 미정
- X. 기타 (직접 서술)

[Answer]:a

## Q5. Code Style — 린터/포매터 + 경계 규약
- A. (권장) FE **Prettier + ESLint(+@typescript-eslint, TypeScript 사용)**, BE **Spotless + google-java-format + Checkstyle** **＋** 네이밍(JS/TS camelCase, Java 표준, DB snake_case) **＋** 경계 매핑 규약: **JSON 필드 camelCase 통일 + JPA snake_case 물리 네이밍 전략** **＋** BE 3계층(Controller/Service/Repository)에서 **Entity를 API 경계로 노출하지 않음** **＋** 전역 에러 응답 스키마 단일화 **커스텀 `{code,message,details}`**
- B. 위와 같되 에러 스키마를 **RFC 7807(`application/problem+json`)** 로
- C. 위와 같되 프론트를 **JavaScript(TS 미사용)** 로
- D. 린터/포매터 도구만 채택하고 경계 규약(네이밍/에러/Entity 노출)은 확정하지 않음
- X. 기타 (직접 서술)

[Answer]:a

## Q6. 보안 자세 (Testing/Code Style 교차, 로컬 한정) — 후속 워크플로우 상속
- A. (권장) 비밀번호는 **적응형 해시(bcrypt, Spring Security 기본)** **＋** **시크릿 비커밋을 hard rule(NEVER)** 로 추가 **＋** 보안 정적분석(BE **SpotBugs+FindSecBugs**, FE **eslint-plugin-security**)과 의존성 스캔(**npm audit + OWASP Dependency-Check** + lockfile 핀)을 team.md 자세로 확정(전부 로컬, 후속 구현 워크플로우 상속) **＋** 안티-중복계정 신호는 **salted hash/마스킹 저장·보관창 후 파기·비노출**로 nfr-requirements/functional-design에 이월
- B. 위와 같되 비밀번호 해시를 **argon2id**로
- C. 최소만 — 비밀번호 해시 + 시크릿 비커밋만 확정, 보안 스캔 자세는 구현 워크플로우로 위임
- X. 기타 (직접 서술)

[Answer]:a

---

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation
프롬프트: "정리된 practice가 맞습니까? 이대로 리드 통합 → team-practices/discovered-rules 확정으로 진행할까요?"
- A. 맞습니다 — 리드 통합 진행
- B. 수정 필요 — 일부 답변을 고치겠습니다

[Answer]: A. 맞습니다 — 리드 통합 진행

---

<!-- §13 Learnings Ritual — pending human turn (blank [Answer] marks genuine human-wait for the Stop hook) -->
## Learnings Ritual
프롬프트: "surface된 후보(c1~c4) 중 harness에 남길 항목을 고르고, 다음을 위해 추가할 메모가 있습니까?"
후보: c1(greenfield/org 기본값 사용), c2(self-guided 편집 재개), c3(greenfield 리뷰→A 매핑), c4(계약 우선을 hard rule로 승격) — 각 `→ project.md ## Corrections`, c4는 team.md 승격 가능.
- 1. 아무것도 남기지 않음 (후보 install 안 함, 메모 없음)
- 2. 후보 선택 (남길 번호 지정; team 승격 여부 포함)
- 3. 메모 추가 (자유 서술 + diary 헤딩 선택: Interpretation/Deviation/Tradeoff/Open question)

[Answer]: 1. 아무것도 남기지 않음
