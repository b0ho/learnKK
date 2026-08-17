# Security Requirements — U1 Contracts & Kernel (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality 관점). Unit=U1 Contracts&Kernel(kind=spec). 출처: business-rules.md(U1 — CC-1 에러 매핑·사번 유일성·인가 규약·네이밍), requirements.md(NFR6 보안·NFR8 아키텍처 경계·FR1.2 bcrypt·FR1.4 사번 유일성·C2 외부 미사용·C5 한국어), team-practices(Security Posture). business-logic-model은 spec Unit에 없어 N/A(consumes_absent). U1은 전 Unit이 상속하는 cross-cutting 보안 계약을 정의 — 도메인별 인가 세부는 소유 Unit. -->

## 개요

U1은 실행 코드가 아니라 계약 Unit이므로, 여기의 보안 요구는 **전 Unit이 상속하는 공통 보안 규약**이다. requirements NFR6(최소 보안)·NFR8(경계 규약)과 team-practices Security Posture를 functional-design의 U1 business-rules(CC-1·인가 규약)와 정합하게 확정한다. 전부 로컬·외부 SaaS 미사용(C2)이라는 제약이 위협 모델을 크게 축소한다.

## 인증 (Authentication)

- **방식:** 세션 토큰 기반(business-rules BR-U1-5의 `Principal{userId,role}`). 무인증 접근은 401.
- **비밀번호 저장:** bcrypt 적응형 해시(requirements FR1.2 [Mandated]). 평문·가역 암호 금지. 실제 해싱은 U2 Auth 소유이나, 계약으로 U1이 "bcrypt 이외 금지" 불변식을 고정.
- **로그인 실패 응답:** 계정 존재 여부를 노출하지 않는 401(business-rules BR-U1-1, component-methods `login` 규약) — 사용자 열거(enumeration) 방지.
- **세션 만료·무효화:** 무효/만료 토큰은 401. 로그아웃 시 세션 무효화(component-methods `logout`). 세션 저장 방식(서버 세션 vs JWT)은 U2 확정 — 보안 요구는 방식 무관하게 "만료·무효화 가능"을 요구.

## 인가 (Authorization) — RBAC 계약

- **모델:** 역할 기반(MENTOR/MENTEE/ADMIN, domain-entities Role). 사용자당 단일 역할(파일럿 [assumption]).
- **규약(전 Unit 상속):** 역할 게이트 위반 403, 소유·참여 경계 위반 403(business-rules BR-U1-5). 관리자 전용 액션(4지점 승인)은 `role != ADMIN` 시 403.
- **최소 권한:** 각 엔드포인트는 필요한 최소 역할·소유 범위만 허용. 열람 경계(참여자만, 자기 모임 멘티만)는 소유 Unit이 집행하되 위반 코드는 403으로 통일.

## 데이터 보호 (Data Protection)

- **민감 데이터:** 비밀번호(해시), 사번(EmployeeNo — 조직 구성원 식별자). 사번은 중복방지 목적으로만 사용하고 불필요 노출을 피한다(requirements NFR6 "안티-중복 신호 목적 한정·비노출").
- **설계상 허용된 tradeoff:** 중복 사번 가입 시 409(`DUPLICATE_EMPLOYEE_NO`)는 특정 사번의 계정 존재를 사실상 확인시킨다. 이는 안티-중복(FR1.4)이 강제하는 설계상 예외로, 사번 열거 위험을 감수하고 중복 방지를 우선한다(계약 모순 아님).
- **전송:** 전부 로컬 환경(C2). 운영 배포 시 TLS는 후속 구현 워크플로우 소관(범위 밖 명시).
- **저장:** 단일 PostgreSQL. 첨부 BLOB(U6). 백업·암호화 at-rest는 이번 범위 밖(NFR4).
- **에러 비노출:** ErrorPayload `message`는 한국어 사용자 메시지(C5)로, 스택트레이스·내부 식별자·SQL 등 내부 정보를 담지 않는다. `details`는 검증 오류 등 안전한 구조화 정보만.

## 입력 검증 (Input Validation)

- 모든 시스템 경계(REST 요청)에서 입력 검증(construction phase guardrail). 검증 실패 400(business-rules BR-U1-1).
- 사번 형식 검증(domain-entities [assumption] 영숫자 4~20자) 위반 400.
- SQL 인젝션 방지: JPA/파라미터 바인딩 사용(문자열 연결 쿼리 금지). 첨부 파일 형식 화이트리스트·크기 상한은 U6 소관.

## 위협 고려 (STRIDE-lite, devsecops 관점)

전부-로컬·파일럿 전제에서 실질 위협만 정리:

| STRIDE | 관련 | 대응(계약 수준) |
|--------|------|-----------------|
| Spoofing | 세션·로그인 | 세션 토큰 검증, 실패 401, 열거 방지 |
| Tampering | 상태 전이·정원 | 불법 전이/중복 409(집행은 소유 Unit), 서버측 권위 검증 |
| Repudiation | 관리자 승인 액션 | 감사 로깅은 범위 축소(파일럿) — 승인 액션 최소 기록 권고 [assumption] |
| Information Disclosure | 사번·피드백·쪽지 | 소유·참여 경계 403, 에러 메시지 비노출 |
| Denial of Service | 파일럿 규모 | 범위 밖(NFR4 단일 인스턴스). size clamp 등 경량 방어만 |
| Elevation of Privilege | RBAC | 서버측 역할 게이트, 클라이언트 신뢰 금지 |

## 컴플라이언스 (compliance 관점)

- 외부 규제 프레임워크(GDPR/PCI 등) 적용 대상 아님 — 외부 SaaS·결제·외부 연동 없음(C2, Out of Scope). 사번은 사내 식별자로 조직 내부 처리.
- 시크릿 비커밋(team-practices Security Posture·requirements NFR6) — `.env`/Spring profile, 저장소에 자격증명·키 금지(construction guardrail).
- 정적분석·의존성 스캔(team-practices Security Posture) 상속 — 실행 파이프라인은 후속 구현 워크플로우.

## 검증 가능성 (quality 관점 — 테스트 시나리오)

- 무인증 접근 → 401 (계약 테스트).
- MENTEE가 관리자 승인 액션 호출 → 403.
- 비참여자가 자료/피드백 열람 → 403.
- 잘못된 비밀번호 로그인 → 401(계정 존재 비노출 메시지 동일).
- 중복 사번 가입 → 409(`DUPLICATE_EMPLOYEE_NO`).
- 이 시나리오들은 team-practices Testing Posture의 API 계약 테스트 계층에서 검증.

## Assumptions & Open Questions

- **[assumption]** 감사 로깅 범위: 파일럿이라 관리자 승인 액션의 최소 기록만 권고 — 정식 감사 추적은 범위 밖.
- **[assumption]** 단일 역할·사번 형식은 domain-entities와 동일 가정.
- **[open]** 세션 저장 방식(서버 세션 vs JWT) U2 확정 — 만료·무효화 요구는 방식 무관.
- **[N/A]** business-logic-model: spec Unit이라 부재(consumes_absent expected:false).
- 운영 TLS·at-rest 암호화·백업은 이번 설계 범위 밖(후속 구현 워크플로우).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U1 Contracts&Kernel, kind=spec). 검토 범위 = U1의 security-requirements.md·tech-stack-decisions.md + consumed(business-rules.md·requirements.md). 반증 시도 후 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정으로 계약·요구 ID를 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 이 계약을 구현 가능한 수준을 무너뜨리는 근거를 세우지 못함.

### 검증 근거 (Verification evidence)

- **인증 계약** — "세션 토큰 + `Principal{userId,role}`, 무인증 401"은 BR-U1-5와 정합. bcrypt는 requirements FR1.2 [Mandated]와 일치. 비열거 401은 BR-U1-1 표(401=로그인 실패·계정 존재 비특정)와 일치. 세션 저장 방식은 [open]으로 U2에 위임 — BR-U1 open questions와 동일 위임선.
- **인가(RBAC) 계약** — 역할 게이트 위반 403, 소유·참여 경계 위반 403, `role != ADMIN` → 403은 BR-U1-5를 정확히 승계. 4지점 승인=관리자 전용은 requirements 역할 정의·FR7 승인 모델과 정합.
- **데이터 보호** — 사번 목적 한정·비노출은 NFR6 문구를 직접 인용. ErrorPayload `message` 한국어(C5)·내부정보 비노출·`details` 안전 구조화는 NFR8 `{code,message,details}` 스키마 및 BR-U1-1과 정합.
- **입력 검증** — 검증 실패 400, 사번 형식(영숫자 4~20자) 위반 400은 BR-U1-1·BR-U1-2와 일치. SQL 인젝션(JPA 파라미터 바인딩)은 construction guardrail 범위 내.
- **STRIDE-lite** — Tampering(불법 전이/중복 409·집행은 소유 Unit), Info Disclosure(경계 403·에러 비노출), DoS(NFR4 단일 인스턴스 범위 밖·size clamp)는 BR-U1-1/BR-U1-6과 모순 없음.
- **소유 경계** — U1은 cross-cutting 계약만 정의하고 세션 저장·Spring Security 구성(TD-6)·도메인별 인가 세부·첨부 화이트리스트(U6)를 소유 Unit에 정확히 위임. spec Unit이므로 performance/scalability/reliability 산출물 부재는 produces_kinds([service]/[service,ui]) 규칙상 정상 — 결함 아님.
- **인식적 상태(Epistemic)** — 감사 로깅 범위·단일 역할·사번 형식은 [assumption], 세션 저장 방식은 [open]으로 유지. 어느 것도 확정 규약으로 암묵 승격되지 않음. business-logic-model은 [N/A](consumes_absent) 명시.
- **컴플라이언스** — "외부 규제 프레임워크 미적용(all-local)"은 C2·Out of Scope(결제·정산·SSO·외부 연동 제외)로 방어 가능. 사내 식별자(사번) 내부 처리 논리 성립.
- **tech-stack** — 확정 스택(React/Spring/PostgreSQL/로컬)은 C1 [Mandated]·NFR1 승계. TD-1~TD-6 모두 U1이 소유할 계약·플랫폼 수준 결정으로 적절(OpenAPI 계약 도구, Flyway baseline, enum varchar+CHECK, 전역 @RestControllerAdvice CC-1 매핑, Jackson 경계 규약, 인증 경계 형태). TD-4/TD-5는 BR-U1-1/BR-U1-4와 정합. TD-3은 BR-U1-3·open questions와 일치. TD-6은 Spring Security 상세를 U2로 위임 — 소유 경계 침범 없음.
- **순환 의존성** — U1은 공유 커널(ErrorPayload/Role/Principal/Pagination — U1 domain-entities 소유)을 정의하고 하류 Unit이 이를 상속·집행. U1이 하류 Unit 구현에 역의존하지 않음 → 계약 그래프에 순환 없음.
- **센서** — H2 헤딩 9개(≥2 충족, required-sections OK). upstream-coverage: business-rules(다수)·requirements(NFR6/NFR8/FR1.2 등)·business-logic-model([N/A]로 명시) 모두 prose에 참조 → 충족.

### Suggestions (non-blocking)

- **S1** — 사번 비노출 원칙과 "중복 사번 → 409 `DUPLICATE_EMPLOYEE_NO`"(FR1.4/BR-U1-2 강제) 사이에는 열거(enumeration) 긴장이 존재한다. 409 응답이 특정 사번의 계정 존재를 사실상 확인시킨다. 이는 안티-중복 요구(FR1.4)가 강제하는 설계상 예외이므로 계약 모순은 아니나, 데이터 보호 절에 "사번 존재는 가입 중복 검증 목적의 409로 노출될 수 있음(설계상 허용된 tradeoff)"을 한 줄 명시하면 인식적 완결성이 높아진다.
- **S2** — TD-1/TD-2/TD-3은 application-design ADR-001/003/004와 team-practices #1/#2/#3을 근거로 인용하나 이들은 이번 pass-list 밖 아티팩트다. 추적성은 인용으로 보존되나, 각 ADR/practice 번호가 해당 결정의 source-of-truth임을 각주로 고정하면 하류 감사 대칭이 개선된다. (계약 결함 아님 → 이월 불요.)
- **S3** — 세션 만료·무효화 계약이 "방식 무관"으로 옳게 기술되나, 로그아웃 시 서버측 무효화 가능성이 JWT 채택 시 blocklist 요구를 유발한다. U2 확정 시 "무효화 요구가 stateless JWT 선택에 부과하는 제약"을 명시적으로 넘겨받도록 open 항목에 한 줄 덧붙이길 권고.

Verdict: READY
