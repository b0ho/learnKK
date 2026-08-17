# Domain Entities — U2 Auth & App Shell (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U2 Auth&App Shell(kind=service, FE 앱 셸 포함). 스토리: US-1.1~1.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U2=C1 인증·사용자+FE 셸), requirements.md(FR1.1~1.5·NFR6/8), components.md(C1 책임·소유 데이터 user/profile/session), component-methods.md(AuthService/UserService 시그니처), services.md(세션 인증·무상태 검증), U1 domain-entities(EmployeeNo·Role·Principal 계약). -->

## 개요

U2는 C1(인증·사용자) 도메인의 엔티티와 FE 앱 셸을 소유한다. 엔티티는 U1이 정의한 공유 타입(Role·EmployeeNo·Principal)을 사용하며, 소유 테이블은 `user`·`profile`·`session`(components.md C1). Entity는 API 경계에 비노출(NFR8) — DTO 경유.

## 엔티티

### User (사용자)

US-1.1 가입, US-1.3 사번 중복방지의 핵심 엔티티.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | U1 baseline 규약 |
| `nickname` | varchar | NOT NULL, **unique** | 로그인 식별자(US-1.2) — 유일성은 로그인 계약이 요구하는 하드 불변식 |
| `passwordHash` | varchar | NOT NULL | bcrypt(FR1.2), 평문 저장 금지 |
| `employeeNo` | varchar | NOT NULL, **unique** | 정규화 저장(U1 EmployeeNo), 중복방지 앵커(FR1.4) |
| `role` | varchar(enum Role) | NOT NULL | MENTOR/MENTEE/ADMIN(U1 Role) |
| `createdAt`/`updatedAt` | timestamptz | | baseline |

- **유일성(하드 불변식):** `employeeNo` unique(중복계정 방지 FR1.4). `nickname` **unique** — component-methods C1의 `login(LoginRequest{nickname, password})`가 nickname을 로그인 식별자로 고정하므로(W2 "nickname으로 User 조회") nickname 유일성은 로그인 결정성을 위해 **반드시 성립해야 하는 계약 불변식**이다(선택적 UX 취향이 아님). 위반 시 가입 단계에서 409 `DUPLICATE_NICKNAME`로 차단. requirements는 사번만 명시하나, 채택된 login-by-nickname 계약이 nickname 유일성을 강제한다.
- **불변:** `employeeNo`는 가입 후 변경 불가. `role`은 가입 시 단일 지정(멀티롤 미지원은 파일럿 [assumption]).

### Profile (프로필)

US-1.4. User와 1:1.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `userId` | BIGINT (FK, PK) | User 1:1 | |
| `interestTags` | text[] 또는 조인 테이블 | 0..n | 관심사 해시태그(FR1.3) |
| `intro` | varchar | 선택, 길이 상한 [assumption] 500자 | 한 줄 소개(FR1.3) |

- 관심사 태그 저장 형태(PostgreSQL `text[]` vs `profile_tag` 조인 테이블)는 [open] — 파일럿은 `text[]` 기본 가정, 태그 검색 요구 생기면 조인 테이블.

### Session (세션)

US-1.2. 로그인 시 발급, 검증·무효화.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id`/`token` | varchar (PK 또는 unique) | 난수 토큰 | 서버 세션 방식 가정 |
| `userId` | BIGINT (FK) | NOT NULL | |
| `role` | varchar(enum Role) | | Principal 구성 캐시 |
| `createdAt` | timestamptz | | |
| `expiresAt` | timestamptz | NOT NULL | 만료 검증 |
| `revokedAt` | timestamptz | nullable | 로그아웃 시 설정 |

- **세션 저장 방식(U1 [open] 확정):** 파일럿은 **서버 세션(DB `session` 테이블) 채택.** 단일 인스턴스라 서버 세션 조회 부담 없음(services.md), 로그아웃 즉시 무효화가 단순(JWT blocklist 불요). JWT는 범위 밖.
- `validateSession(token)` → 만료·revoked 아니면 `Principal{userId, role}` 반환, 아니면 401(U1 BR-U1-5).

## 관계

- User 1:1 Profile, User 1:N Session. Profile·Session은 User 삭제 시 함께 제거(파일럿엔 사용자 삭제 흐름 없음 — 보존).
- User.role은 U1 Role enum 참조. 타 Unit은 User를 직접 접근하지 않고 `UserService`/`AuthService`(component-methods C1) 경유.

## 생명주기 (User)

- 가입(활성) → (로그인/로그아웃 반복) → (파일럿엔 비활성화·탈퇴 흐름 없음, [open] 후속).

## FE 앱 셸 데이터 (참고)

앱 셸은 자체 영속 엔티티가 없고, 로그인 후 `Principal`(세션)과 프로필을 클라이언트 상태로 보유. 상세 화면·상태 설계는 business-logic-model.md.

## Assumptions & Open Questions

- **[decided]** `nickname` 유일성은 login-by-nickname 계약(component-methods C1)이 강제하는 하드 불변식 — 가정이 아님. requirements가 사번만 명시했더라도 채택된 로그인 방식이 이를 요구.
- **[assumption]** `intro` 길이 상한(500자)은 UX 판단 추가 — requirements 미명시.
- **[assumption]** 단일 역할·사번 형식은 U1과 동일. 역할 값은 가입 요청(확장된 `SignupRequest.role ∈ {MENTOR, MENTEE}`)에서 받음(business-rules BR-U2-1) — ADMIN은 이 경로 거부.
- **[open]** 관심사 태그 저장 형태(text[] vs 조인 테이블) — 파일럿 text[] 가정.
- **[decided]** 세션 저장 = 서버 세션(DB) — U1의 [open] 항목을 U2에서 확정.
- 사용자 탈퇴·비활성화 흐름은 범위 밖.
