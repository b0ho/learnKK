# Security Requirements — U2 Auth & App Shell (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U2(service). 출처: business-logic-model.md(W1~W4·세션·FE), business-rules.md(BR-U2-1~6), requirements.md(NFR6 보안·NFR8 경계·FR1.2 bcrypt·FR1.4 사번). U1 보안 계약(security-requirements) 상속·집행. U2는 인증/인가의 단일 소유자라 보안이 가장 중대한 Unit. -->

## 개요

U2는 U1 cross-cutting 보안 계약을 상속하고, 인증·세션·RBAC의 **실 집행자**다. 인증은 전 시스템 보안의 뿌리이므로 여기의 요구가 다른 Unit의 인가를 뒷받침한다.

## 인증 보안

- **비밀번호:** bcrypt 해시(FR1.2 [Mandated]), cost 인자로 적응형 강도. 평문·가역·약한 해시(MD5/SHA-1) 금지. 비밀번호는 로그·에러·응답에 절대 미노출.
- **로그인 열거 방지:** 실패 시 계정 존재 비특정 401(BR-U2-3, U1 BR-U1-1). 아이디/비밀번호 오류를 구분하지 않음.
- **무차별 대입(brute force):** 파일럿 기본 방어 — 로그인 실패 속도제한/락아웃 [assumption] 권고(예: N회 실패 시 지연). 정식 구현은 후속.

## 세션 보안

- **토큰:** CSPRNG 기반 난수(≥128비트, 예측 불가) — 약한 `Random` 금지. 서버 세션(DB) 저장(domain-entities [decided]).
- **만료·무효화:** `expiresAt` 만료·`revokedAt`(로그아웃) 시 401. 즉시 무효화 가능(서버 세션 이점).
- **클라이언트 저장:** sessionStorage [assumption] — XSS 노출 여지. 운영 하드닝 시 httpOnly 쿠키 재검토(discharge 필요). 전부-로컬 파일럿이라 NFR6 최소 수준 수용.
- **전송:** 로컬 환경(C2). 운영 TLS는 후속.

## 인가 보안 (RBAC 집행)

- 매 보호 요청 `validateSession` → `Principal` 주입. 무인증 401.
- 역할 게이트·소유 경계 위반 403(BR-U2-4, U1 BR-U1-5). **서버 권위** — FE 역할 적응형 렌더는 UX 보조일 뿐 신뢰 대상 아님(BR-U2-6).
- ADMIN 계정 생성은 일반 가입 경로 차단(BR-U2-1a, 400 `ADMIN_SIGNUP_FORBIDDEN`) — 권한 상승 방지.

## 데이터 보호

- 사번(EmployeeNo): 목적 한정·비노출(NFR6). 단, 중복 가입 시 409 `DUPLICATE_NICKNAME`/`DUPLICATE_EMPLOYEE_NO`가 존재를 확인시키는 설계상 tradeoff(FR1.4 강제, U1 security-requirements와 동일 입장).
- 입력 검증 400(BR-U2-1). SQL 인젝션 방지: JPA 파라미터 바인딩.
- 에러 메시지 내부정보 비노출(U1).

## STRIDE (U2 초점)

| STRIDE | U2 대응 |
|--------|---------|
| Spoofing | 난수 토큰·bcrypt·열거 방지 401 |
| Tampering | 서버측 세션·역할 권위 검증 |
| Repudiation | 로그인/로그아웃 최소 기록 권고 [assumption] |
| Info Disclosure | 비밀번호/사번 비노출, 에러 비노출 |
| DoS | 로그인 속도제한 권고(파일럿 경량) |
| Elevation of Privilege | ADMIN 가입 차단, 서버 역할 게이트 |

## 컴플라이언스

- 외부 규제 프레임워크 미적용(C2 all-local, U1 compliance와 동일). 사번=사내 식별자.
- 시크릿 비커밋(team-practices), 정적분석·의존성 스캔 상속.

## 검증 시나리오 (quality)

- 잘못된 비밀번호 → 401(동일 메시지). 만료 토큰 → 401. MENTEE의 관리자 액션 → 403. `role=ADMIN` 가입 → 400. 중복 사번/닉네임 → 409. bcrypt 해시 저장 확인(평문 아님).

## Assumptions & Open Questions

- **[assumption]** 로그인 속도제한/락아웃, bcrypt cost, sessionStorage(운영 재검토), 최소 감사 기록.
- **[open]** 세션 슬라이딩 갱신, httpOnly 쿠키 전환(운영).
- 운영 TLS·정식 감사·WAF는 이번 범위 밖(후속).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U2 Auth & App Shell, kind=service incl. React app shell). 검토 범위 = U2의 5개 nfr 산출물(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + 상속한 U1 계약(security-requirements·tech-stack-decisions). 반증 우선 — 참조 깨짐·순환 의존·계약 모순·파일럿 제약 위반을 사냥한 뒤 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정으로 요구 ID를 교차 대조하고, 서버 세션 결정이 U1 계약과 충돌하는지·과잉설계 NFR이 파일럿 제약을 위반하는지·인식적 태그가 암묵 승격되는지를 추궁했으나, 개발자가 아키텍트 추가 질의 없이 이 NFR을 구현 가능한 수준을 무너뜨리는 근거를 세우지 못함.

### 검증 근거 (Verification evidence)

- **functional-design 정합** — bcrypt(FR1.2 [Mandated]), 비열거 401(BR-U2-3·U1 BR-U1-1), RBAC 403(BR-U2-4·U1 BR-U1-5), nickname/employeeNo 유일성→409(`DUPLICATE_NICKNAME` BR-U2-1b / `DUPLICATE_EMPLOYEE_NO` BR-U2-2), ADMIN 가입 거부(BR-U2-1a, 400 `ADMIN_SIGNUP_FORBIDDEN`)가 모두 business-rules·business-logic-model의 규칙 ID·상태코드와 정확히 일치. 인용된 BR/FR/NFR ID 전부 consumed 파일에서 해소됨 — dangling reference 없음.
- **U1 상속·무모순** — U2는 U1 cross-cutting 보안 계약(CC-1 400/401/403/409 매핑, 비열거 401, `Principal{userId,role}`, 에러 내부정보 비노출)을 상속·집행하며 어느 항목도 재정의·약화하지 않음. 사번 열거 tradeoff(409가 사번 존재 확인)는 U1 security-requirements와 동일 입장으로 명시 — 로그인 비열거(401)와 가입 중복(409)의 분리는 U1이 이미 수용한 설계상 예외이므로 계약 모순 아님.
- **U1 [open] 정당 해소(승격 아님)** — U1 TD-6은 세션 저장 방식(서버 세션 vs JWT)을 명시적으로 U2에 위임([open], "만료·무효화 가능"만 요구). U2 TD-U2-2가 서버 세션(DB)을 확정한 것은 **소유 Unit의 결정**으로, U1이 요구한 즉시 무효화(`revokedAt`)를 충족 → 위임의 정당한 해소이지 암묵 승격이 아님. JWT 미채택으로 U1 S3의 blocklist 제약도 자연 소거.
- **파일럿 NFR 현실성(과잉설계 없음)** — 성능 목표는 SLA가 아닌 가이드(NFR3 1~2초)로 명시; session-check <50ms는 매 보호요청 전처리(validateSession, token unique 인덱스 단건 조회)라는 근거로 자기부과된 tighter 값이며 인덱스 조회로 달성 가능 → 허위 목표 아님. scalability(단일 JVM·단일 DB·수평확장/오토스케일 범위 밖)는 NFR4와, reliability(SLA/HA 없음·데이터 무결성=트랜잭션+unique)는 NFR4·NFR5와 정합. C2 로컬 전제상 어느 NFR도 파일럿 제약을 초과하지 않음.
- **보안 깊이(devsecops)** — bcrypt cost 10~12(로그인 <1초 범위), 서버 세션 즉시 무효화, sessionStorage XSS 명시적 인정+httpOnly 재검토 discharge 경로, brute-force 속도제한/락아웃 권고, ADMIN 권한상승 차단(가입 400 + 서버 역할 게이트 이중)까지 위협별 대응이 STRIDE 표에 매핑됨. 토큰은 헤더 전송(쿠키 아님)이라 CSRF 비해당 — 누락 아님. 파일럿 최소보안(NFR6) 하에서 deferral(TLS·WAF·정식 감사)은 정당하며 모두 명시적.
- **인식적 상태(Epistemic)** — bcrypt cost·rate limiting·sessionStorage·세션 TTL·lazy 세션 정리·수평확장 세션 스토어가 전부 [assumption]/[open]으로 태깅됨. 서버 세션만 [decided](U1 위임 해소). 확정 규약으로의 은밀한 승격 없음.
- **참조·순환** — U2→U1 상속은 단방향(U1이 U2에 역의존하지 않음, U1은 방식 무관 계약만 고정) → 순환 없음. 인용된 모든 ID 해소.
- **센서** — required-sections: performance 5·security 9·scalability 5·reliability 6·tech-stack 5 H2(모두 ≥2). upstream-coverage: 5개 파일 전부 source-comment에 business-logic-model·business-rules·requirements 3종 참조. 펜스드 TS/JS/TSX 스니펫 없음 → linter/type-check nothing to flag. produces_kinds(service): performance/scalability/reliability 3종 모두 존재.

### Suggestions (non-blocking)

- **S1 — 토큰 엔트로피 정량화.** "충분한 엔트로피의 난수"를 구현이 해석 가능하도록 "CSPRNG, ≥128비트" 한 줄을 명시하면 개발자가 약한 `Random`을 쓰는 foot-gun을 사전 차단할 수 있음(현재도 방향은 옳음, 정량 보강 권고).
- **S2 — session.role 캐시 staleness.** `SessionResponse{token, role}`·세션 캐시된 role은 향후 역할 변경 시 재로그인 전까지 stale. functional-design에 이미 [open]으로 있으므로 여기 reliability/security에 "역할 변경 시 재로그인 요구" 한 줄 링크만 남기면 인식적 대칭이 개선됨.
- **S3 — DUPLICATE_NICKNAME 열거.** 데이터 보호 절이 사번 409 열거 tradeoff는 명시하나 nickname 409도 동일 벡터(민감도는 낮음). "닉네임 존재도 가입 409로 노출될 수 있으나 표시명이라 수용" 한 줄 추가 시 완결성 향상. (계약 결함 아님 → 이월 불요.)

Verdict: READY
