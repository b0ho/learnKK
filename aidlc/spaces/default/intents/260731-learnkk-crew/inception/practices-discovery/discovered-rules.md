# Discovered Rules — learnKK (런크크)

<!-- practices-discovery 리드 초안(DRAFT). 확정된 프로젝트 결정(project.md의 Tech Stack·Decided·Corrections, constraint-register)에서 명백히 도출되는 hard constraint만 담는다. 사람의 확정이 필요한 규칙은 여기 넣지 않고 인터뷰로 미룬다. Mandated = `ALWAYS ...`, Forbidden = `NEVER ...`. -->

이 문서는 **확정된 프로젝트 결정에서 명백히 도출되는 hard constraint만** 최소로 담은 초안이다. 사람의 확정(affirmation)이 아직 필요한 practice 성격의 규칙은 의도적으로 제외했고, 인터뷰에서 다룬다.

## Mandated

- ALWAYS 프론트엔드는 React, 백엔드는 Java Spring, 데이터 저장은 PostgreSQL로 구현한다 (project.md Tech Stack lock, feasibility 확정).
- ALWAYS 시스템 전 구성요소를 로컬 환경에서 완결하도록 설계·구축한다 (constraint-register T3, project.md Tech Stack lock).
- ALWAYS 사용자 비밀번호는 해시하여 저장한다 (project.md Decided, feasibility R1).
- ALWAYS 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고, 고유명사·기술 용어는 영어를 그대로 쓴다 (project.md Corrections).

## Forbidden

- NEVER 외부 SaaS·클라우드·AWS 등 외부 서비스에 의존하는 설계를 도입한다 (constraint-register T3, 전부 로컬 확정).
- NEVER 이번 워크플로우의 범위에 실제 구현 코드·배포·CI·운영 인프라를 포함한다 (constraint-register T5, scope-document out-of-scope; 이번은 설계 전용).
- NEVER 안티-중복계정용으로 수집한 신호(IP 등)를 목적 외로 쓰거나, 최소 보관·비노출 원칙을 벗어나 노출한다 (project.md Decided, feasibility R3).

## Assumptions & Open Questions

- 여기에는 확정 결정에서 직접 도출되는 hard rule만 담았다. Testing coverage floor, 브랜칭·squash 정책, 린터/포매터 강제 등 practice 성격 규칙은 사람 확정 전까지 mandated/forbidden으로 승격하지 않고 인터뷰로 미뤘다.
- 인터뷰에서 팀이 확정하면 추가 `ALWAYS/NEVER` 규칙이 affirmation gate에서 project.md로 승격될 수 있다.
