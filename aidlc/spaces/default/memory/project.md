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

## Deployment

<!-- Project-specific specialisation. -->

## Code Style

<!-- Project-specific specialisation. -->

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
## Scope Overrides

<!-- Custom scope rules for this project. -->

## Forbidden

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: NEVER [behavior] (affirmed [date]) -->
<!-- Example: NEVER throw exceptions across service layer boundaries (affirmed 2026-05-17) -->

## Mandated

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: ALWAYS [behavior] (affirmed [date]) -->
<!-- Example: ALWAYS use Result<T,E> for fallible operations in service layer (affirmed 2026-05-17) -->

## Corrections

<!-- Project-specific corrections from human feedback. -->
<!-- Format: NEVER/ALWAYS [behavior] (learned [date]) -->
- 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고, 고유어·기술 용어(고유명사·기술 용어 등)는 영어를 그대로 사용한다 (learned 2026-07-31) <!-- cid:intent-capture:u1 -->
- §13 learnings 반영을 사용자에게 물어볼 때, 추천 항목을 목록 최상위에 별도 항목으로 명시한다 (learned 2026-07-31) <!-- cid:intent-capture:u2 -->
- 승인 게이트를 제시하는 시점에, 그 스테이지와 관련된 변경사항만 미리 git staged 상태로 만들어 두고(무관한 변경은 stage하지 않음), 사용자가 승인하면 staged 변경을 커밋한다 (learned 2026-07-31) <!-- cid:market-research:u1 -->
