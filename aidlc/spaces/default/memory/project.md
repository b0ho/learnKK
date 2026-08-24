# Project-Level Rules

> Project-specific specialisation and corrections. Loaded after `org.md` and
> `team.md` as strict-additive guidance; contradictions with broader policy
> are rejected. Populated by practices-discovery and the self-learning loop.
>
> Use sparingly: most teams don't need a project layer. Reach for it
> only when this specific project needs stable, durable guidance beyond the
> team practice (for example, package-specific release checks or an additional
> regression suite for a legacy component).

## Way of Working

<!-- Project-specific specialisation. Example: -->
<!-- This monorepo requires package-scoped branch names and a package owner -->
<!-- review in addition to the team's normal merge policy. -->

## Walking Skeleton

<!-- Project-specific specialisation. Example: -->
<!-- The walking skeleton must exercise the legacy service adapter as well -->
<!-- as the new service boundary. -->

## Testing Posture

<!-- Project-specific specialisation. -->

- 신규 모듈/도메인은 슬라이스 테스트(@WebMvcTest·@Mock)만으로 검증을 끝내지 말고, 최소 1개의 부팅형 검증(통합 테스트 또는 실행 앱 대상 라이브 E2E)으로 ApplicationContext·JPA·JPQL 배선을 확인한다 — 슬라이스는 빈 등록·엔티티명·JPQL 파싱을 커버하지 못한다 (learned 2026-08-24) <!-- cid:code-generation:boot-verification -->

## Deployment

<!-- Project-specific specialisation. -->

## Code Style

<!-- Project-specific specialisation. -->

- 신규 도메인 타입(JPA 엔티티·Spring Data 리포지토리)의 simple name은 기존 도메인(특히 auth의 `Session`/`SessionRepository`)과 겹치지 않게 도메인 접두를 붙인다 — 같은 simple name은 Spring 빈 이름·JPA 엔티티명·JPQL 참조가 전역 네임스페이스에서 충돌해 기동 실패를 유발한다 (예: U5는 `MeetingSession`/`MeetingSessionRepository`, 테이블 `meeting_session`) (learned 2026-08-24) <!-- cid:code-generation:name-collision -->
- controlled 입력(React)의 표시값을 매 키 입력마다 파싱된 배열/구조에서 되도출하지 말 것 — 입력 도중 구분자 문자(쉼표 등)가 즉시 제거되어 타이핑이 불가능해진다. 로컬 raw-text 상태를 두고 표시하며, 파싱(trim·필터) 결과만 부모/상위 상태에 전달한다 (예: SurveyBuilder의 CHOICE 선택지 `ChoiceOptionsInput`) (learned 2026-08-24) <!-- cid:code-generation:controlled-input-rawtext -->

## Tech Stack

<!-- Technology choices locked for this project. -->

- 프론트엔드 React, 백엔드 Java Spring, 데이터 저장 PostgreSQL, 전부 로컬 환경 (feasibility에서 팀 확정) (learned 2026-07-31) <!-- cid:feasibility:c1 -->
## Decided

<!-- Decisions made in earlier stages that should not be re-asked. -->
<!-- Format: DECIDED: [decision] (Stage [slug], [date]) -->

- 보안은 최소 수준(비밀번호 해시 저장) + 승인 없는 가입이므로 히든 안티-중복계정 장치(IP 등 신호 활용, 목적 한정·최소보관·비노출)를 둔다 (Stage feasibility, 2026-07-31) (learned 2026-07-31) <!-- cid:feasibility:c2 -->
- 관리자(시스템 관리자)는 4개 지점에서 승인한다 — ① 모임 개설, ② 모임 시작, ③ 멘토 정상 완료, ④ 멘티 수료 완료 (Stage rough-mockups, 2026-07-31) (learned 2026-07-31) <!-- cid:rough-mockups:u1 -->
- 멘티 수료는 출석률 80% 기준으로 시스템이 자동 판정하고 관리자가 승인(④)하여 확정한다. 멘토는 멘티 수료를 인정하지 않으며, 멘토는 모임 정상 완료 인정 신청(관리자 승인③)과 멘티 피드백 확인만 담당한다 (Stage rough-mockups, 2026-07-31) (learned 2026-07-31) <!-- cid:rough-mockups:u2 -->
- 멘티 피드백(과정 설문)은 멘토가 확인하며 시스템 관리자도 열람할 수 있다 (Stage rough-mockups, 2026-07-31) (learned 2026-07-31) <!-- cid:rough-mockups:u3 -->
- 사전 설문(신청 설문)의 문항 틀은 멘토가 모임 개설 시 자유롭게(문항을 임의로 구성) 지정하며, 멘티는 신청 시 그 설문에 응답한다 (Stage rough-mockups, 2026-07-31) (learned 2026-07-31) <!-- cid:rough-mockups:u4 -->
- 크로스모듈에서 '활성(active) 신청 관계'는 enrollment status=APPLIED로 판정한다 — U4 현행 enum의 유일 비취소 리터럴이며 시작·완료 후에도 유지됨. U7 messaging의 canMessage(멘토=자기 모임 활성 멘티 / 멘티=신청 모임 멘토)가 이를 사용. U4에 시작후 상태가 추가되면 이 판정을 재검토한다. (learned 2026-08-23) <!-- cid:code-generation:c5 -->
## Scope Overrides

<!-- Custom scope rules for this project. -->

- 이 프로젝트는 개별 개발자의 로컬 구현 작업이다. 구현 워크플로우(intent)에서도 **파이프라인 단계(ci-pipeline 3.7, deployment-pipeline 4.1)와 operation phase 전체(4.1~4.7)는 실행하지 않는다.** 구현 스코프의 유효 종료 지점은 construction의 build-and-test(3.6)다. (learned 2026-08-17)

## Forbidden

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: NEVER [behavior] (affirmed [date]) -->
<!-- Example: NEVER throw exceptions across service layer boundaries (affirmed 2026-05-17) -->

- NEVER 외부 SaaS·클라우드·AWS 등 외부 서비스에 의존하는 설계를 도입한다 (constraint-register T3, 전부 로컬 확정). (affirmed 2026-08-17)
- NEVER 이번 워크플로우의 범위에 실제 구현 코드·배포·CI·운영 인프라를 포함한다 (constraint-register T5, scope-document out-of-scope; 이번은 설계 전용). (affirmed 2026-08-17)
- NEVER 안티-중복계정용으로 수집한 신호(IP 등)를 목적 외로 쓰거나, 최소 보관·비노출 원칙을 벗어나 노출한다 (project.md Decided, feasibility R3). (affirmed 2026-08-17)
- NEVER 비밀번호·DB 자격증명·세션 시크릿 등 비밀값을 repo에 커밋한다 — `.env`류는 gitignore, 환경변수/Spring profile로 주입 (devsecops `[affirmed]` Q6, secure-by-default). (affirmed 2026-08-17)
- NEVER JPA Entity를 API 경계(Controller 응답) 밖으로 그대로 노출한다 — Controller는 Request/Response DTO만 사용 (developer `[affirmed]` Q5). (affirmed 2026-08-17)
- NEVER 파이프라인(ci-pipeline·deployment-pipeline)과 operation phase 단계(배포·환경 프로비저닝·관측·인시던트·성능검증·피드백)를 실행한다 — 개별 개발자 로컬 구현 작업이며 구현은 build-and-test(3.6)에서 종료한다 (learned 2026-08-17). <!-- cid:code-generation:no-pipeline-ops -->
## Mandated

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: ALWAYS [behavior] (affirmed [date]) -->
<!-- Example: ALWAYS use Result<T,E> for fallible operations in service layer (affirmed 2026-05-17) -->

- ALWAYS 프론트엔드는 React, 백엔드는 Java Spring, 데이터 저장은 PostgreSQL로 구현한다 (project.md Tech Stack lock, feasibility 확정). (affirmed 2026-08-17)
- ALWAYS 시스템 전 구성요소를 로컬 환경에서 완결하도록 설계·구축한다 (constraint-register T3, project.md Tech Stack lock). (affirmed 2026-08-17)
- ALWAYS 사용자 비밀번호는 적응형 해시(bcrypt, Spring Security 기본)로 저장한다 — 평문·가역 암호 저장 금지 (project.md Decided, feasibility R1, devsecops `[affirmed]` Q6). (affirmed 2026-08-17)
- ALWAYS 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고, 고유명사·기술 용어는 영어를 그대로 쓴다 (project.md Corrections). (affirmed 2026-08-17)
- ALWAYS JSON API 필드는 camelCase로 통일하고, JPA 물리 네이밍은 snake_case 전략으로 매핑한다 (developer `[affirmed]` Q5 — 병렬 interface 불일치 방지). (affirmed 2026-08-17)
- ALWAYS 전역 에러 응답은 단일 커스텀 스키마 `{code, message, details}`로 통일한다 (developer `[affirmed]` Q5). (affirmed 2026-08-17)
## Corrections

<!-- Project-specific corrections from human feedback. -->
<!-- Format: NEVER/ALWAYS [behavior] (learned [date]) -->
- 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고, 고유어·기술 용어(고유명사·기술 용어 등)는 영어를 그대로 사용한다 (learned 2026-07-31) <!-- cid:intent-capture:u1 -->
- §13 learnings 반영을 사용자에게 물어볼 때, 추천 항목을 목록 최상위에 별도 항목으로 명시한다 (learned 2026-07-31) <!-- cid:intent-capture:u2 -->
- 승인 게이트를 제시하는 시점에, 그 스테이지와 관련된 변경사항만 미리 git staged 상태로 만들어 두고(무관한 변경은 stage하지 않음), 사용자가 승인하면 staged 변경을 커밋한다 (learned 2026-07-31) <!-- cid:market-research:u1 -->
- 산출물 수정 시 변경과 무관한 부분까지 파일 전체를 재작성(fs_write)하지 말 것 — str_replace로 실제 바뀌는 부분만 최소 범위로 편집해 staged 대비 diff를 리뷰 가능하게 유지한다 (learned 2026-08-17) <!-- cid:user-stories:user-diff-hygiene -->
