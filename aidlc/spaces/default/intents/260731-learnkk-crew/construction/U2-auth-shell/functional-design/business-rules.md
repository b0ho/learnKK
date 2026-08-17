# Business Rules — U2 Auth & App Shell (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U2 Auth&App Shell(service). 스토리 US-1.1~1.4(unit-of-work-story-map.md). 출처: unit-of-work.md(U2), requirements.md(FR1.1~1.5·NFR6/8), components.md(C1), component-methods.md(AuthService/UserService), services.md(세션·무상태 검증), U1 business-rules(CC-1·사번 유일성·인가 규약)·U1 domain-entities(EmployeeNo/Role/Principal). U2는 U1 cross-cutting 규칙을 상속하고 인증·사용자 도메인 규칙을 확정. -->

## 개요

U2는 U1 계약(CC-1 에러 매핑, 사번 유일성 불변식 BR-U1-2, RBAC 인가 규약 BR-U1-5)을 **집행**한다. 여기서는 가입·로그인·세션·프로필의 도메인 규칙을 확정한다.

## BR-U2-1. 회원가입 (US-1.1)

- 입력: 확장된 `SignupRequest{nickname, password, employeeNo, role}`. **가입 승인 절차 없음**(FR1.1) — 즉시 활성 계정 생성.
  - **[계약 확장 주석]** component-methods C1의 `signup(SignupRequest{nickname, password, employeeNo})`에는 role 필드가 없다. U2는 가입 시 역할 지정을 위해 `SignupRequest`에 **제약된 `role ∈ {MENTOR, MENTEE}` 필드를 추가**한다(BR-U2-1a). 이 확장은 U1 소유 #1 OpenAPI 계약에 반영되어야 한다(하류 정합화 항목).
- 검증: 필수값 누락·형식 위반 400. `employeeNo`는 U1 EmployeeNo 형식(정규화 후 영숫자 4~20자 [assumption]) 위반 시 400.
- 비밀번호: bcrypt 해시 저장(FR1.2 [Mandated]). 평문·가역 저장 금지. 비밀번호 최소 강도 [assumption] 8자 이상(파일럿 최소 규칙).

### BR-U2-1a. 가입 시 역할 지정 (role provenance)

- 역할 값의 출처는 **확장된 `SignupRequest.role`** — 요청에서 명시적으로 받는다(파생·기본값 아님). 허용 값은 `MENTOR` 또는 `MENTEE`.
- `role = ADMIN` 요청은 일반 가입 경로에서 **거부(400 `ADMIN_SIGNUP_FORBIDDEN`)**. 관리자 계정은 시드/운영자 지정 경로로만 생성 [assumption](일반 가입과 분리).
- 허용 집합 외 값은 400.

### BR-U2-1b. 닉네임 유일성 (로그인 계약 강제)

- `nickname`은 로그인 식별자(component-methods C1 `login(nickname, ...)`)이므로 **유일해야 한다**(하드 불변식, 선택적 UX 취향 아님 — 위반 시 로그인 비결정성 발생).
- 집행: DB `user.nickname` unique + 애플리케이션 선검증. 중복 시 **409 `DUPLICATE_NICKNAME`**. 동시 가입 경합은 unique 위반을 409로 변환.

## BR-U2-2. 사번 기반 중복방지 (US-1.3, FR1.4)

- 하나의 사번당 계정 1개. 정규화(대문자·공백제거) 후 유일성 판정(U1 BR-U1-2).
- 집행: DB `user.employee_no` unique 제약이 최종 보증 + 애플리케이션 선검증. 중복 시 **409 `DUPLICATE_EMPLOYEE_NO`**.
- 동시 가입 경합: unique 제약 위반을 409로 변환(경합-안전). 히든 IP 신호 방식 폐기(requirements A4).

## BR-U2-3. 로그인·세션 (US-1.2)

- `login(nickname, password)`: bcrypt 검증. 실패 시 **401**(계정 존재 비특정 — 동일 메시지, 열거 방지, U1 BR-U1-1·NFR6).
- 성공 시 `Session` 발급(토큰·만료). 응답 `SessionResponse{token, role}`.
- `validateSession(token)`: 만료(`expiresAt` 경과)·무효(`revokedAt` 설정) 시 401. 유효 시 `Principal{userId, role}`.
- `logout(token)`: `revokedAt` 설정으로 즉시 무효화(서버 세션).
- 세션 만료 시간 [assumption]: 파일럿 기본 예: 발급 후 12시간(구현 확정). 슬라이딩 갱신 여부는 [open].

## BR-U2-4. RBAC 집행 (US-1.2, FR1.5)

- 모든 보호 엔드포인트는 유효 세션 필요(무인증 401, U1 BR-U1-5).
- 역할 게이트: 관리자 전용 액션 `role != ADMIN` → 403. 멘토 전용(개설 등)·멘티 전용(신청 등) 위반 403. 실제 도메인 게이트는 해당 Unit이 판정하되, U2는 `Principal`(역할) 제공·검증 미들웨어를 소유.

## BR-U2-5. 프로필 (US-1.4)

- `getProfile(userId)`: 본인 또는 열람 권한자. 프로필 공개 범위 [assumption] — 파일럿은 로그인 사용자 간 기본 조회 가능(관심사·소개는 공개 정보).
- `updateProfile(userId, {tags, intro})`: 본인만. 태그 개수 상한 [assumption] 10개, 소개 길이 상한 500자 — 초과 400.
- `nickname`·`employeeNo`는 프로필 수정 대상 아님(불변, BR-U2-1/US-1.3).

## BR-U2-6. FE 앱 셸 인가 규칙

- 미인증 사용자는 로그인/가입 화면만 접근. 보호 라우트 접근 시 로그인으로 리다이렉트(클라이언트 가드) — 단, **서버가 권위**(클라이언트 가드는 UX용, U1 BR-U1-5).
- 3탭(모임/내 러닝/내정보) 및 화면은 `role`에 따라 적응형 렌더(멘토/멘티/관리자 다른 액션 노출). 서버 인가와 이중 방어.

## 에러 처리 (U1 CC-1 상속)

- 400 검증, 401 인증, 403 인가, 409 중복. ErrorPayload `{code,message,details}`. `message` 한국어(C5).

## Assumptions & Open Questions

- **[assumption]** 비밀번호 최소 강도(8자), 세션 만료(12h), 태그/소개 상한, ADMIN 생성 경로 분리 — 파일럿 기본. 조직 정책 확정 시 교체.
- **[decided]** nickname 유일성(BR-U2-1b)·SignupRequest.role 확장(BR-U2-1a)은 채택된 로그인·역할 계약이 강제 — 가정 아님. #1 OpenAPI 계약에 반영 필요.
- **[open]** 세션 슬라이딩 갱신 여부, 프로필 공개 범위 정책.
- 사용자 탈퇴·비밀번호 재설정 흐름은 범위 밖(requirements 미명시).
