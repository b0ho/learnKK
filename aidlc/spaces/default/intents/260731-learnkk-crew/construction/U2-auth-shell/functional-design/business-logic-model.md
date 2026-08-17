# Business Logic Model — U2 Auth & App Shell (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U2 Auth&App Shell(service, FE 셸 포함). 스토리 US-1.1~1.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U2), requirements.md(FR1.x·NFR1 모바일 웹뷰·NFR8), components.md(C1·FE feature 구조·단일 API client), component-methods.md(AuthService/UserService), services.md(REST/세션), U1(EmployeeNo/Role/Principal/ErrorPayload). 워크플로우·알고리즘·FE 앱 셸 상호작용을 정의. -->

## 개요

U2의 백엔드 워크플로우(가입·로그인·세션·프로필)와 FE 앱 셸(3탭·라우팅·단일 API client·인증 화면)을 정의한다. 알고리즘은 U1 계약과 U2 business-rules를 절차로 표현.

## 백엔드 워크플로우

### W1. 회원가입 (US-1.1 / signup)

입력은 확장된 `SignupRequest{nickname, password, employeeNo, role}` (role ∈ {MENTOR, MENTEE}; component-methods C1 시그니처의 U2 확장, BR-U2-1a — #1 OpenAPI 계약에 반영 필요).

```
signup(nickname, password, employeeNo, role):
  1. 입력 검증(필수·형식). 실패 → 400
     role ∉ {MENTOR, MENTEE} → 400 (ADMIN 요청은 ADMIN_SIGNUP_FORBIDDEN)
  2. employeeNo 정규화(upper, trim)
  3. employeeNo 유일성 선검증(존재? → 409 DUPLICATE_EMPLOYEE_NO)
     nickname 유일성 선검증(존재? → 409 DUPLICATE_NICKNAME)  # 로그인 계약 강제(BR-U2-1b)
  4. password → bcrypt 해시
  5. User insert(role). DB unique(employeeNo·nickname) 위반 시(경합) → 409
  6. 빈 Profile 생성(1:1)
  7. return UserResponse(비밀번호·해시 제외)
```

### W2. 로그인 (US-1.2 / login)

```
login(nickname, password):
  1. nickname으로 User 조회
  2. 미존재 또는 bcrypt 불일치 → 401 (동일 메시지, 열거 방지)
  3. Session 생성(token=난수, expiresAt=now+TTL)
  4. return SessionResponse{token, role}
```

### W3. 세션 검증 (validateSession) — 모든 보호 요청 전처리

```
validateSession(token):
  1. token으로 Session 조회. 미존재 → 401
  2. revokedAt 설정됨 또는 expiresAt < now → 401
  3. return Principal{userId, role}
```

- 구현: Spring 인터셉터/필터로 보호 라우트 전처리. `Principal`을 요청 컨텍스트에 주입 → 타 Unit 컨트롤러가 인가에 사용.

### W4. 로그아웃 / 프로필 (US-1.2 / US-1.4)

```
logout(token): Session.revokedAt = now
getProfile(userId): 요청자=Principal(세션). 파일럿은 로그인 사용자면 조회 허용 → ProfileResponse
updateProfile(userId, {tags, intro}):
  본인 확인(path userId == Principal.userId, 불일치 → 403 BR-U1-5)
  → 검증(태그≤10, 소개≤500) → 실패 400 → 갱신 → return
```

## FE 앱 셸 (React, 모바일 웹뷰)

### 구조 (components.md feature 기반)

- `api/`: **단일 API client** — fetch 래퍼. 인증 헤더 자동 첨부, ErrorPayload(U1) 해석(코드→사용자 메시지), 401 시 세션 만료 처리(로그인 이동).
- `features/auth/`: 로그인·가입·내정보(프로필) 화면·훅.
- `components/ui/`: shadcn/ui 공통(Form, Input, Button, Card, Badge, Dialog, Tabs 등) — 앱 셸이 셋업.
- `routes/`: 3탭 네비(모임 / 내 러닝 / 내정보) + 로그인/가입(비인증) 라우트. 역할 적응형.

### 상호작용 흐름

- **비인증:** 진입 → 로그인 화면. 가입 링크 → 가입 폼. 성공 시 세션 토큰 저장(메모리+지속 저장 [assumption] sessionStorage), 3탭 홈으로.
- **인증:** 하단 3탭 네비. `내정보` 탭에서 프로필 조회·수정, 로그아웃.
- **역할 적응형 렌더:** `Principal.role`로 탭·액션 노출 제어. 예) 멘토=모임 개설 버튼, 멘티=신청 버튼, 관리자=승인 큐. 서버 인가와 이중 방어(BR-U2-6).
- **세션 만료(401):** API client가 감지 → 토큰 폐기 → 로그인 화면 + 안내.

### 폼 검증 (클라이언트, 서버 재검증 전제)

- 가입: 닉네임/비밀번호/사번 필수 + 역할 선택(MENTOR/MENTEE 라디오, ADMIN 미노출), 사번 형식(영숫자 4~20자), 비밀번호 최소 8자. 클라이언트 즉시 피드백 + 서버 400 매핑. 닉네임 중복은 서버 409(`DUPLICATE_NICKNAME`) 매핑.
- 프로필: 태그 ≤10, 소개 ≤500자.
- 접근성(CC-2·NFR7): 폼 라벨·에러 텍스트 연결, 키보드 포커스, 색 대비(상식 수준).

## 데이터 흐름·통합 지점

- 타 Unit은 `AuthService.validateSession`(Principal)·`UserService.getProfile`를 in-process Service 호출로 사용(services.md 오케스트레이션). U2는 인증/사용자의 단일 소유자.
- FE 단일 API client는 전 feature가 공유 — U2가 셋업하고 U3~U9 화면이 재사용.

## 에러·엣지 케이스

- 중복 사번/닉네임 동시 가입 경합 → DB unique 위반 → 409(W1 step5).
- 만료 직전 요청 → validateSession에서 만료 판정 401(경계: `expiresAt < now`).
- 잘못된 토큰·변조 → 401.
- 프로필 미존재(데이터 이상) → 404(정상 흐름에선 가입 시 생성되어 발생 안 함).

## Assumptions & Open Questions

- **[assumption]** 토큰 클라이언트 저장 = sessionStorage(파일럿 — XSS 노출 여지, 운영 하드닝 시 httpOnly 쿠키 재검토), 세션 TTL 12h.
- **[decided]** nickname 유일성·SignupRequest.role 확장은 로그인·역할 계약이 강제(business-rules BR-U2-1a/1b) — #1 OpenAPI 계약 반영 필요.
- **[open]** 슬라이딩 세션 갱신, 관심사 태그 저장 형태(domain-entities), ADMIN 계정 생성 경로, 세션 role 캐시 변경 시 재로그인 요구.
- FE 빌드 도구(Vite 가정)에 따라 테스트 러너(Vitest) 확정(team-practices).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — re-review iteration 2 (adversarial functional-design review, Unit U2 Auth & App Shell, kind=service incl. React app shell; scope: business-logic-model.md + business-rules.md + domain-entities.md against the six consumed inception contracts and the U1 shared kernel U2 inherits)

Iteration 1 returned NOT-READY on two blocking findings (B1 nickname-uniqueness framing, B2 signup role provenance). I re-hunted both — trying to prove the "fix" left a residual escape hatch or introduced a new contradiction — and then swept for regressions. Both blockers are fully resolved and no new blocker surfaced.

### B1 — nickname uniqueness (RESOLVED)

The invariant is now stated as hard and load-bearing, consistently across all three files, with the droppable escape removed:

- **domain-entities.md** — User.`nickname` is `NOT NULL, unique`, annotated "유일성은 로그인 계약이 요구하는 하드 불변식"; the "유일성(하드 불변식)" prose ties it to `login(LoginRequest{nickname, password})` and W2's "nickname으로 User 조회," explicitly "선택적 UX 취향이 아님." Reclassified **[decided]** in Assumptions ("가정이 아님"). The iter-1 "미채택 시 표시명 중복 허용" escape is gone.
- **business-rules.md** — new **BR-U2-1b** "닉네임 유일성 (로그인 계약 강제)": hard invariant, DB `user.nickname` unique + app pre-check, duplicate → **409 `DUPLICATE_NICKNAME`**, race resolved via unique-violation→409. Assumptions block marks it **[decided]**.
- **business-logic-model.md** — W1 step 3 pre-checks nickname uniqueness (→ 409 `DUPLICATE_NICKNAME`, "# 로그인 계약 강제(BR-U2-1b)"); step 5 DB unique(employeeNo·nickname) is race-safe; FE maps server 409 `DUPLICATE_NICKNAME`. Assumptions **[decided]**.

`DUPLICATE_NICKNAME` is now justified (login determinism requires the invariant), follows U1's `<DOMAIN>_<REASON>` UPPER_SNAKE code convention (BR-U1-1), and the whole-file grep for the old "미채택/중복 허용" framing returns only the (now-replaced) iter-1 review text — zero residual in the design body.

### B2 — signup role provenance (RESOLVED)

Role provenance is now defined and consistent across all three files, and the contract divergence is surfaced rather than silent:

- **business-rules.md** — BR-U2-1 input is the extended `SignupRequest{nickname, password, employeeNo, role}` with an explicit **[계약 확장 주석]** noting component-methods C1's `signup(...)` has no role field and that U2 adds a constrained `role ∈ {MENTOR, MENTEE}`, flagged for reconciliation into the U1-owned **#1 OpenAPI** contract. New **BR-U2-1a** pins provenance: value comes from `SignupRequest.role` (not derived/defaulted), `role = ADMIN` → **400 `ADMIN_SIGNUP_FORBIDDEN`**, out-of-set → 400.
- **business-logic-model.md** — W1 signature updated to `signup(nickname, password, employeeNo, role)`; step 1 rejects `role ∉ {MENTOR, MENTEE}` (400, ADMIN → `ADMIN_SIGNUP_FORBIDDEN`); FE signup form adds a MENTOR/MENTEE radio with ADMIN hidden; the extension is flagged "#1 OpenAPI 계약에 반영 필요" in Assumptions [decided].
- **domain-entities.md** — Assumptions state the role value is received from the extended `SignupRequest.role ∈ {MENTOR, MENTEE}` (ref BR-U2-1), ADMIN rejected on this path; the User.role column still admits U1's full {MENTOR/MENTEE/ADMIN} Role enum (ADMIN via the separate seed path), which is internally consistent.

A developer can now implement signup role assignment without asking the architect. The `ADMIN_SIGNUP_FORBIDDEN` → 400 choice (rather than 403) is correct under U1 CC-1: there is no authenticated Principal at signup, so this is constrained-field validation, not an authorization gate.

### Regression check — clean

- **U1 kernel fidelity intact.** EmployeeNo, Role, Principal{userId,role}, ErrorPayload, and the 400/401/403/404/409 CC-1 mapping still match the U1 shared kernel; 사번 uniqueness still enforced at U2 (DB unique + pre-check → 409 `DUPLICATE_EMPLOYEE_NO`) per BR-U1-2. The new `DUPLICATE_NICKNAME`/`ADMIN_SIGNUP_FORBIDDEN` codes both conform to the U1 BR-U1-1 code convention and status mapping.
- **Method fidelity restored.** W1–W4 match component-methods C1 for login/validateSession/logout/getProfile/updateProfile; the sole intentional divergence (signup +role) is documented as a contract extension with a reconciliation obligation, not a silent change.
- **No new contradictions.** The two edits are localized (W1 input/step 1, BR-U2-1/1a/1b, User table + Assumptions). Server-session decision, FE app-shell interactions, error/edge cases, and the profile rules are unchanged and remain consistent. Epistemic tags are honest: firm-contract items are now [decided]; genuine pilot choices (bcrypt-only min length, 12h TTL, tag/intro caps, sessionStorage, ADMIN seed path, tag storage form) remain [assumption]/[open].
- **Sensors — PASS.** required-sections: business-logic-model 7 H2, business-rules 8 H2, domain-entities 6 H2 (all ≥2). upstream-coverage: every file's source-comment header references all six consumed artefacts (unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services). No fenced TS/JS/TSX snippets → linter/type-check nothing to flag.

### Suggestions (non-blocking, carried from iter 1)

- **S1 — Session `role` cache staleness.** `session.role` caches `Principal.role`; a future role change would leave the session stale until re-login. Out of pilot scope; a one-line "role change requires re-login" note would pre-empt a foot-gun. (Already listed as [open].)
- **S2 — Token in `sessionStorage` (XSS).** JS-readable storage is XSS-reachable; acceptable for the local pilot and tagged [assumption]. A production hardening pass (httpOnly cookie) should discharge it deliberately.
- **S3 — `getProfile(userId)` requester provenance.** Stating the requester derives from `Principal` (symmetric with `updateProfile`'s self-check) would make the read-authorization model explicit.

Verdict: READY
