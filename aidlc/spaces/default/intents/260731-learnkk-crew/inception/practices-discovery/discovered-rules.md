# Discovered Rules — learnKK (런크크)

<!-- practices-discovery 최종본(FINAL). 확정된 프로젝트 결정(project.md·constraint-register)에서 도출되는 hard constraint + 인터뷰(Q1~Q6 전부 A)에서 사람이 확정해 승격 가능해진 durable rule을 담는다. Mandated = `ALWAYS ...`, Forbidden = `NEVER ...`. 인터뷰 확정 규칙은 affirmation gate에서 team.md/project.md로 승격된다. -->

이 문서는 **확정된 프로젝트 결정 + 인터뷰에서 사람이 확정한 durable rule**을 담은 최종본이다. `[affirmed]`는 이번 인터뷰에서 팀이 확정해 승격 가능해진 규칙이다.

## Mandated

- ALWAYS 프론트엔드는 React, 백엔드는 Java Spring, 데이터 저장은 PostgreSQL로 구현한다 (project.md Tech Stack lock, feasibility 확정).
- ALWAYS 시스템 전 구성요소를 로컬 환경에서 완결하도록 설계·구축한다 (constraint-register T3, project.md Tech Stack lock).
- ALWAYS 사용자 비밀번호는 적응형 해시(bcrypt, Spring Security 기본)로 저장한다 — 평문·가역 암호 저장 금지 (project.md Decided, feasibility R1, devsecops `[affirmed]` Q6).
- ALWAYS 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고, 고유명사·기술 용어는 영어를 그대로 쓴다 (project.md Corrections).
- ALWAYS JSON API 필드는 camelCase로 통일하고, JPA 물리 네이밍은 snake_case 전략으로 매핑한다 (developer `[affirmed]` Q5 — 병렬 interface 불일치 방지).
- ALWAYS 전역 에러 응답은 단일 커스텀 스키마 `{code, message, details}`로 통일한다 (developer `[affirmed]` Q5).

## Forbidden

- NEVER 외부 SaaS·클라우드·AWS 등 외부 서비스에 의존하는 설계를 도입한다 (constraint-register T3, 전부 로컬 확정).
- NEVER 이번 워크플로우의 범위에 실제 구현 코드·배포·CI·운영 인프라를 포함한다 (constraint-register T5, scope-document out-of-scope; 이번은 설계 전용).
- NEVER 안티-중복계정용으로 수집한 신호(IP 등)를 목적 외로 쓰거나, 최소 보관·비노출 원칙을 벗어나 노출한다 (project.md Decided, feasibility R3).
- NEVER 비밀번호·DB 자격증명·세션 시크릿 등 비밀값을 repo에 커밋한다 — `.env`류는 gitignore, 환경변수/Spring profile로 주입 (devsecops `[affirmed]` Q6, secure-by-default).
- NEVER JPA Entity를 API 경계(Controller 응답) 밖으로 그대로 노출한다 — Controller는 Request/Response DTO만 사용 (developer `[affirmed]` Q5).

## Assumptions & Open Questions

- 위 `[affirmed]` 규칙은 인터뷰(Q5/Q6 = A)에서 팀이 확정했고, affirmation gate에서 project.md `ALWAYS/NEVER`로 승격 후보다. 이들은 독립 병렬의 interface 불일치·보안 기본값을 직접 낮추는 durable rule이라 project 레이어에 값한다.
- 테스트 프레임워크·coverage floor·린터/포매터·docker-compose 같은 practice 성격 결정은 hard rule이 아니라 team-practices(team.md) 자세로 확정되어 상속한다.
- 안티-중복계정 신호의 보관 형태·보관 창·비노출 경계 상세는 nfr-requirements/functional-design로 이월한다.
