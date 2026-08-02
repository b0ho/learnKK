**Collaborator:** aidlc-developer-agent

## Contribution

developer/code-style 렌즈로 리드 초안(team-practices.md / discovered-rules.md / evidence.md)을 **독립 검토**했다. greenfield라 실측 코드 증거는 없고, 근거는 project.md(Tech Stack lock: React + Java Spring + PostgreSQL, 전부 로컬, 설계 전용), constraint-register, team-assessment(3인 풀스택 · 기능 수직 슬라이스 · 독립 병렬, 핵심 리스크 = 단위 간 interface 불일치)다.

검토 결론: 리드 초안의 다섯 섹션 특화는 방향이 맞고, Code Style에서 스택별 린터/포매터·네이밍 관례를 제안한 점도 타당하다. 다만 이 팀의 지배적 리스크(**독립 병렬에서의 interface 불일치**)를 실제로 줄이려면 "린터를 건다"보다 **공유 기반의 계약을 코드-레벨에서 어떻게 고정하느냐**가 훨씬 결정적이다. 리드 초안은 계약 우선을 way of working 전제로 언급했지만, 그 계약을 담을 **파일/프로젝트 조직 형태**와 **네이밍·레이어·에러 매핑 규약**이 비어 있다 — 세 명이 프론트~백을 각자 통째로 짜는 구조에서 이 규약들이 사전에 합의되지 않으면 병합 시점에 DTO 필드명, HTTP 상태코드, 에러 응답 형태, DB 컬럼명이 제각각으로 어긋난다. 이 세 가지(조직·네이밍·에러 계약)를 인터뷰가 반드시 확정해야 할 항목으로 아래에 구체화한다.

또한 discovered-rules 초안은 hard constraint를 보수적으로만 담았는데, 스택 네이밍 관례(JS/TS camelCase, Java 표준, PostgreSQL snake_case)와 그 **경계 매핑 규약**은 병렬 리스크에 직결되므로 인터뷰에서 사람이 확정하면 project.md의 `ALWAYS`로 승격할 가치가 있다고 본다(초안이 이를 인터뷰로 미룬 판단 자체는 옳다).

## Positions

### 1. 네이밍 컨벤션 — 계층별 관례 + 경계 매핑을 명시적으로 고정 (제안)
- FE(React/TS): 변수·함수·prop `camelCase`, 컴포넌트·타입·인터페이스 `PascalCase`, 상수 `UPPER_SNAKE`. 파일명 규약(컴포넌트 `PascalCase.tsx` vs `kebab-case`)은 팀 확정 필요.
- BE(Java Spring): 표준 Java — 클래스 `PascalCase`, 메서드/필드 `camelCase`, 상수 `UPPER_SNAKE`, 패키지 lowercase.
- DB(PostgreSQL): 테이블·컬럼 `snake_case`. 복수형 vs 단수형 테이블명은 팀 확정 필요.
- **경계 매핑 규약(핵심)**: `DB snake_case ↔ JPA/Entity camelCase ↔ JSON API` 사이의 변환 지점을 한 곳으로 고정해야 한다. 제안: JSON 직렬화는 `camelCase`로 통일(Jackson `PropertyNamingStrategy` 명시), JPA는 `@Column` 매핑 또는 물리 네이밍 전략(snake_case)으로 일괄 처리. 이 규약이 없으면 FE가 `snake_case`/`camelCase`를 추측하게 되어 인터페이스 불일치의 1순위 원인이 된다.
- **인터뷰 질문**: (a) JSON 필드는 camelCase로 통일하는가? (b) JPA 물리 네이밍 전략을 snake_case로 강제하는가? (c) DB 테이블명 단/복수?

### 2. 레이어 경계 — 계약 우선 구조를 코드-레벨로 규정 (제안)
- BE: `Controller → Service → Repository` 3계층을 강제하고, **계층 간 전달 객체를 분리**한다 — Controller는 Request/Response DTO만, Service는 도메인 객체, Repository는 Entity. Entity를 Controller까지 그대로 노출하지 않는다(직렬화 사고·과노출 방지).
- FE: presentational 컴포넌트와 상태/데이터 접근(hook·store) 분리. API 호출은 컴포넌트에 흩지 않고 **단일 API client 계층**으로 모은다 — 이 client가 곧 BE 계약의 FE측 소비 지점이 된다.
- **병렬 리스크 연결**: 공통 기반(인증·모임 도메인)의 Service 인터페이스와 DTO 형태를 계약으로 먼저 고정하면, 세 명이 그 계약 위에서 독립적으로 자기 슬라이스를 구현할 수 있다.
- **인터뷰 질문**: Entity를 API 경계 밖으로 절대 노출하지 않는 규칙을 `NEVER`로 확정할지?

### 3. 에러 처리 규약 — 통일된 에러 응답 계약 (제안, 병렬 리스크 직결)
- BE: Spring `@ControllerAdvice`/`@RestControllerAdvice`로 전역 예외 처리를 한 곳에 두고, **표준 에러 응답 스키마**(예: `{ code, message, details }` 또는 RFC 7807 `application/problem+json`)를 하나로 고정. HTTP 상태코드 사용 규약(검증 실패 400, 인증 401, 인가 403, 미존재 404)을 문서화.
- FE: API client가 이 에러 스키마를 단일 지점에서 해석해 사용자 메시지로 매핑. 사용자 노출 메시지는 project.md 규약에 따라 한글.
- 예외를 삼키지 않는다(fail loud). Service 경계에서 checked/unchecked 예외 정책을 팀이 정한다.
- **인터뷰 질문**: (a) 에러 응답 스키마를 무엇으로 고정할지(커스텀 vs RFC 7807)? (b) 이 스키마를 계약 산출물로 먼저 확정해 세 슬라이스가 공유할지?

### 4. 파일/프로젝트 조직 — monorepo + 공유 기반을 계약 아티팩트로 (강한 제안)
- **monorepo 권장**: 3인 전원 풀스택 · 독립 병렬 구조에서 FE와 BE를 한 repo에 두면 공유 계약(API 스펙, DTO/타입, DB 스키마)을 한 소스에서 버전 관리하고 원자적으로 변경할 수 있어 인터페이스 drift가 줄어든다. 예: `/frontend`(React), `/backend`(Spring, Gradle/Maven 멀티모듈 가능), `/contracts` 또는 `/docs/api`.
- **공유 기반을 interface contract로 명시화**: 인증 + 모임 도메인 + DB 스키마를 다음 세 계약 아티팩트로 **구현 전 고정**하기를 권한다 —
  1. **API 계약**: OpenAPI 스펙(엔드포인트·요청/응답 DTO·상태코드·에러 스키마) 한 파일.
  2. **DB 스키마 계약**: 초기 스키마 + 마이그레이션 규약(예: Flyway/Liquibase 도구 선택). 컬럼명·타입·제약을 여기서 확정.
  3. **도메인 타입 계약**: 공유 도메인 용어집(모임/멘토/멘티/출석/수료 상태값 enum 등)을 FE·BE가 동일 이름으로 참조.
- 이 세 계약이 delivery-planning에서 각 개발자 슬라이스보다 **먼저** 확정·소유자 배정되어야 병렬 리스크가 실질적으로 통제된다(초안이 이 순서 확정을 delivery-planning으로 넘긴 판단은 맞으나, "무엇을 계약으로 볼지"는 여기서 못박아 두는 게 좋다).
- **인터뷰 질문**: (a) monorepo vs multi-repo? (b) API 계약을 OpenAPI로 먼저 작성해 계약으로 삼을지? (c) DB 마이그레이션 도구(Flyway/Liquibase/수기)? (d) BE는 Gradle vs Maven, 멀티모듈 vs 단일모듈?

### 5. 린터/포매터 — 구체 도구 확정 (리드 제안 지지 + 구체화)
- FE: **Prettier + ESLint**(TypeScript 사용 시 `@typescript-eslint`), repo 루트 `.prettierrc`/`eslint.config.js`. 리드 제안 지지.
- BE: **Spotless + google-java-format**를 포매터로, **Checkstyle**을 규칙 린터로. Gradle/Maven 플러그인으로 build에 연결. 리드 제안 지지 — 다만 설계 전용 워크플로우라 실제 설정 파일 생성은 이후 구현 워크플로우로 이월된다.
- 규약 자체(도구·버전·설정)를 계약 아티팩트와 함께 monorepo 루트에 두어 세 사람이 동일 포맷으로 커밋하게 한다(diff 노이즈·스타일 충돌 방지).
- **인터뷰 질문**: 위 도구 세트를 그대로 채택할지, TypeScript를 쓸지(JS-only면 ESLint 규칙 조정), coverage/lint를 (이후 워크플로우의) 병합 게이트로 강제할지.

### 6. discovered-rules 승격 후보 (인터뷰 확정 시)
- 사람이 확정하면 다음을 project.md `ALWAYS`로 승격 제안: 스택별 네이밍 관례 + JSON camelCase 통일, 전역 에러 응답 스키마 단일화, Entity의 API 경계 노출 금지(`NEVER`). 이들은 병렬 리스크를 직접 낮추는 durable rule이라 project 레이어에 값한다. 초안이 이를 미리 승격하지 않고 인터뷰로 미룬 것은 절차상 옳다.
