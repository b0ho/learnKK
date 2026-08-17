# Tech Stack Decisions — U1 Contracts & Kernel (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·quality 관점). Unit=U1 Contracts&Kernel(kind=spec). 출처: business-rules.md(U1 — 네이밍·에러·인가 규약), requirements.md(C1 스택 고정·NFR8 경계·NFR1 모바일 웹뷰), project.md Tech Stack(React+Spring+PostgreSQL 로컬 확정), team-practices(Testing Posture·Code Style·Deployment), application-design ADR-001~007. business-logic-model은 spec Unit에 없어 N/A. U1은 전 Unit이 상속하는 계약·플랫폼 수준 기술 결정을 확정 — 도메인별 라이브러리 선택은 소유 Unit. -->

## 개요

스택 자체는 상류에서 확정(requirements C1 [Mandated], project.md Tech Stack): **React(FE) + Java Spring(BE) + PostgreSQL(DB), 전부 로컬.** U1은 여기에 더해 전 Unit이 상속하는 **계약·플랫폼 수준 기술 결정**(3계약 도구·경계 규약)을 고정한다. 각 결정은 근거와 함께 기록한다.

## 확정 스택 (상류 [Mandated] 상속)

| 계층 | 선택 | 근거 |
|------|------|------|
| 프론트엔드 | React + TypeScript, shadcn/ui | C1·project.md, refined-mockups design-system. 모바일 웹뷰 우선(NFR1). |
| 백엔드 | Java + Spring Boot(모놀리스) | C1·project.md. 모듈러 모놀리스(ADR-001). 3계층(team-practices). |
| DB | PostgreSQL(단일) | C1·project.md. 모듈별 테이블 소유, 첨부 BLOB(ADR-004). |
| 실행 | 로컬(docker-compose) | C2 외부 미사용, team-practices Deployment. |

## U1이 소유하는 계약 수준 기술 결정

### TD-1. API 계약 도구 — OpenAPI (#1)

- **결정:** REST API 계약을 **OpenAPI 스펙 단일 소스**로 관리(team-practices #1). JSON camelCase(NFR8), 전역 에러 ErrorPayload.
- **근거:** FE-BE interface 불일치가 최대 리스크(team-practices) → 스펙 우선·계약 테스트(Testing Posture)로 고정. 병렬 착수의 하드 선행.
- **Reversibility:** 낮음(전 Unit이 의존). 초기 고정 필요.

### TD-2. DB 마이그레이션 — Flyway (#2)

- **결정:** Flyway `V{n}__{desc}.sql`(ADR-003). U1이 `V1__baseline.sql`(공통 컬럼·enum 규약) 소유, 각 Unit이 자기 테이블 마이그레이션 추가.
- **근거:** 스키마 계약(#2)의 버전 관리·재현성. snake_case 물리 네이밍(NFR8).
- **Reversibility:** 중간(마이그레이션 추가는 자유, baseline 규약 변경은 파급).

### TD-3. 도메인 타입 표현 — enum varchar+CHECK (#3)

- **결정:** 공유 enum(MeetingStatus/CompletionStatus/Role)을 PostgreSQL **varchar + CHECK 제약**으로 물리 표현(정수 코드 아님). JSON은 enum 명 문자열 직렬화.
- **근거:** 가독성·마이그레이션 안전(값 추가가 PG enum 타입보다 유연). domain-entities 기본 가정.
- **대안:** PostgreSQL `enum` 타입 — 타입 안전하나 값 변경이 ALTER TYPE로 무거움. 파일럿엔 varchar+CHECK 채택.
- **Reversibility:** 중간.

### TD-4. 에러 처리 — 전역 @RestControllerAdvice

- **결정:** 도메인 예외를 CC-1 매핑(400/401/403/404/409)으로 변환하는 전역 `@RestControllerAdvice`(component-methods, business-rules BR-U1-1).
- **근거:** 컨트롤러 중복 제거·일관된 ErrorPayload 보장.

### TD-5. 직렬화·경계 — Jackson camelCase / Entity 비노출

- **결정:** JSON camelCase(Jackson), JPA snake_case, Controller는 DTO만(NFR8·team-practices). Entity를 API 경계에 노출 금지.
- **근거:** 경계 규약 통일(NFR8), 내부 모델 변경의 API 파급 차단.

### TD-6. 인증 기술 — 세션 토큰(방식은 U2)

- **결정(계약 수준):** 세션 토큰 인증, `Principal{userId,role}` 반환. 구체 구현(Spring Security 구성, 서버 세션 vs JWT)은 U2 확정.
- **근거:** 단일 인스턴스라 서버 세션도 무리 없음(services.md). U1은 경계 형태만 고정.

## 테스트 도구 (team-practices Testing Posture 상속 — 참고)

U1이 새로 정하지 않고 team-practices에서 상속(전 Unit 공통):
- 백엔드: JUnit 5 + Spring Boot Test, MockMvc(`@WebMvcTest`), Mockito.
- 프론트: React Testing Library + Vitest.
- 통합: Testcontainers 로컬 PostgreSQL.
- API 계약 테스트: OpenAPI 스펙 기반 응답 스키마 검증(#1 계약 리스크 대응).

## 범위 밖 (out of scope, 후속 구현 워크플로우)

- CI/CD 플랫폼(GitHub Actions 등), 배포 파이프라인, 운영 모니터링 — requirements C3.
- TLS/at-rest 암호화, 백업 자동화, HA — NFR4.

## Assumptions & Open Questions

- **[assumption]** enum 물리 표현은 varchar+CHECK 기본 — 구현에서 PG enum으로 바꿀 여지.
- **[open]** 세션 저장 방식(서버 세션 vs JWT), Spring Security 상세 구성은 U2.
- **[open]** 빌드 도구(Vite vs webpack/CRA)가 Vitest vs Jest 선택을 좌우(team-practices) — 구현에서 확정.
- **[N/A]** business-logic-model: spec Unit이라 부재(consumes_absent expected:false).
