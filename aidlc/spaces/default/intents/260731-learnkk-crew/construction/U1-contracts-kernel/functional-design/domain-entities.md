# Domain Entities — U1 Contracts & Kernel (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U1 Contracts&Kernel(kind=spec). 출처: unit-of-work.md(U1 = C0 Shared Kernel + 3계약 스캐폴딩), unit-of-work-story-map.md(U1은 스토리 비소유·계약 소유), requirements.md(FR1.x·FR2.3·FR7.1·NFR8·C5), components.md(C0 정의), component-methods.md(DTO/enum 기준·CC-1 에러 규약), services.md(REST/JSON camelCase·에러 스키마·세션 인증). U1은 spec Unit이라 런타임 엔티티가 아니라 전 Unit이 참조하는 공유 타입 계약(#3)·DB baseline(#2)·OpenAPI 스캐폴딩(#1)을 정의한다. 실제 도메인 엔티티(user/meeting/session...)는 각 소유 Unit의 domain-entities에서 정의. -->

## 개요

U1은 코드 실행 산출물이 아니라 **계약(contract) 정의 Unit**이다. components.md의 C0 Shared Kernel 책임과 team-practices의 3계약(#1 OpenAPI · #2 DB 스키마 · #3 도메인 타입)을 functional-design 수준으로 확정하여, U2~U9가 병렬 착수할 때 참조할 단일 소스를 고정한다. 여기서 정의한 enum·값객체·에러 스키마·역할은 unit-of-work.md가 명시한 "U1 선고정이 병렬 착수의 하드 선행"을 충족한다.

- **소유 범위(#3 도메인 타입):** 모임 상태 enum, 수료 상태 enum, RBAC 역할 enum, 공통 에러 스키마, 공통 값객체(사번 등). 순수 타입·상수만 — 비즈니스 로직 없음(전이 규칙의 *집행*은 U3, 여기는 *정의*).
- **소유 범위(#2 DB baseline):** Flyway 초기 마이그레이션(V1) — 공유 enum의 물리 표현 규약과 전 모듈 공통 컬럼 규약. 각 모듈 테이블 자체는 소유 Unit이 자기 마이그레이션으로 추가.
- **소유 범위(#1 OpenAPI):** 전역 규약 스캐폴딩(에러 응답 스키마, 인증 헤더, 페이지네이션 파라미터 형식, camelCase 네이밍) — 엔드포인트 본체는 각 Unit이 계약에 맞춰 채움.

## 도메인 열거형 (Enums) — #3 계약

### MeetingStatus (모임 상태)

requirements FR2.3 상태 흐름의 정본. 값과 한국어 표시명:

| enum 값 | 한국어 표시 | 설명 |
|---------|-------------|------|
| `PENDING_APPROVAL` | 개설신청 | 멘토가 개설, 관리자 ① 대기 |
| `RECRUITING` | 모집중 | ① 승인 완료, 멘티 선착순 신청 접수 |
| `READY_TO_START` | 시작대기 | 모집 확정 완료, ② 대기 |
| `IN_PROGRESS` | 진행중 | ② 시작 승인, 세션·출석 활성 |
| `COMPLETED` | 완료 | ③ 관리자 직접 완료 처리 |
| `REJECTED` | 반려 | ① 반려(종료 상태) |
| `CANCELLED` | 취소 | 모집 미달 확정 취소 등(종료 상태) |

- 물리 표현(#2): PostgreSQL `varchar` + `CHECK` 제약(enum 명 문자열) 또는 도메인 타입. 정수 코드 아님(가독성·마이그레이션 안전).
- 전이 그래프·전이 규칙·불법 전이 409 집행은 **U3 Meeting**의 business-rules 소관. U1은 값 집합만 고정.

### CompletionStatus (멘티 수료 상태)

requirements FR7.1 자동판정→관리자 확정(④) 흐름.

| enum 값 | 한국어 표시 | 설명 |
|---------|-------------|------|
| `NOT_COMPLETED` | 미수료 | 기본값·80% 미충족 |
| `COMPLETION_CANDIDATE` | 수료후보 | 출석율 80% 이상 자동 판정 |
| `COMPLETED` | 수료확정 | 관리자 ④ 승인 |

- 판정 로직(a*100 ≥ 80*S)·전이는 **U5 Session/Attendance** business-rules 소관. U1은 값 집합만 고정.

### Role (RBAC 역할)

requirements FR1.5 3역할.

| enum 값 | 한국어 표시 | 설명 |
|---------|-------------|------|
| `MENTOR` | 멘토 | 모임 개설·운영 |
| `MENTEE` | 멘티 | 신청·참여·출석 |
| `ADMIN` | 시스템 관리자 | 4지점 승인·모니터링 |

- 역할은 사용자당 단일(파일럿). 한 사용자가 멘토이자 멘티일 수 있는지는 [assumption]: 파일럿에서는 **가입 시 단일 역할** 가정, 멀티롤은 범위 밖. U2 Auth가 세션 Principal에 role을 실어 전달.

## 값 객체 (Value Objects) — #3 계약

### EmployeeNo (사번)

requirements FR1.1/FR1.4(중복계정 방지=사번 유일성), A5(형식은 functional-design 확정).

- **의미:** 조직 구성원 식별자. 하나의 사번당 계정 1개(유일성)로 중복 가입 차단.
- **형식 [assumption]:** 파일럿 기준 **영숫자 4~20자, 공백 없음, 대소문자 구분 없이 정규화(upper)** 후 저장·비교. 조직 실제 사번 체계 확정 시 U2 Auth에서 정규식 교체(예: 고정 자릿수). 유일성 검증은 U2 소유(DB unique 제약 + 애플리케이션 선검증).
- **불변성:** 가입 후 변경 불가(계정 식별 앵커).

### ErrorPayload (공통 에러 스키마)

requirements NFR8·stories CC-1·services.md 전역 에러. 전 REST 응답의 에러 본문 형태(#1 계약).

```
ErrorPayload {
  code: string      // 도메인 에러 코드(예: "MEETING_INVALID_TRANSITION", "ENROLLMENT_FULL")
  message: string   // 사용자 노출용 한국어 메시지(C5: 한국어)
  details?: object   // 선택 — 필드별 검증 오류 등 구조화 정보
}
```

- HTTP 상태코드 매핑은 business-rules.md의 CC-1 규약 참조.
- `code`는 안정 식별자(FE 분기용), `message`는 표시용. FE 단일 API client가 이 스키마를 해석(components.md `api/`).

### Pagination (목록 파라미터 규약)

component-methods.md "페이지네이션·정렬은 functional-design/구현 구체화" 이월 항목을 U1에서 전역 규약으로 고정(#1).

- 요청: `page`(0-base, 기본 0), `size`(기본 20, 상한 100), `sort`(옵션, `field,asc|desc`).
- 응답 래퍼: `{ content: T[], page, size, totalElements, totalPages }` (JSON camelCase).
- 파일럿 규모(NFR2 수십 건)라 기본 미적용도 허용하나, 목록 엔드포인트는 이 규약을 따른다.

## 인증·세션 규약 (전 Unit 공통 참조)

services.md "세션 저장(DB 세션 vs JWT)은 functional-design 확정" 이월. U1은 **경계 형태만** 고정(구현 선택은 U2):

- 인증은 **세션 토큰(HTTP 헤더 `Authorization: Bearer <token>` 또는 세션 헤더)**. 무상태 검증.
- `Principal { userId, role }` — U2 `validateSession`이 반환하는 공유 타입. 전 모듈이 인가 판단에 사용.
- 세션 저장 방식(서버 세션 vs JWT)은 U2 domain-entities에서 확정 — U1은 Principal 형태만 계약.

## DB 스키마 Baseline (#2 계약)

- **마이그레이션 도구:** Flyway(application-design ADR-003), `V{n}__{desc}.sql` 규약. U1은 `V1__baseline.sql`에 공유 규약을 둔다:
  - 공통 컬럼 규약: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `created_at timestamptz NOT NULL DEFAULT now()`, `updated_at timestamptz`.
  - 물리 네이밍: snake_case(NFR8). enum 컬럼은 varchar + CHECK.
- 각 모듈 테이블(user/profile/session/meeting/enrollment/session/attendance/post/message/...)은 **소유 Unit의 마이그레이션**으로 추가. U1은 baseline과 규약만 소유(테이블 소유 경계는 components.md).

## 프론트엔드 앱 셸 접점 (참고 — 실체는 U2)

U1 자체는 UI를 소유하지 않는다(spec). 다만 FE 단일 API client(components.md `api/`)가 U1의 ErrorPayload·Pagination·인증 헤더 규약에 의존하므로, U2 App Shell이 이 규약을 client 계층에 반영한다.

## Assumptions & Open Questions

- **[assumption] 사번 형식(A5):** 영숫자 4~20자·정규화 저장으로 가정. 조직 실제 체계 확정 시 U2에서 교체. (requirements A5 미확정 유지)
- **[assumption] 단일 역할:** 사용자당 역할 1개. 멀티롤(멘토겸멘티)은 범위 밖.
- **[open] 세션 저장 방식:** 서버 세션 vs JWT는 U2 domain-entities에서 확정. U1은 Principal 형태만 계약.
- enum의 물리 표현(varchar+CHECK vs PostgreSQL enum 타입) 최종 선택은 U1 business-rules/구현에서 확정 — baseline은 varchar+CHECK 기본 가정.
- 이 문서는 계약 정의이며 실제 코드는 이번 워크플로우 범위 밖(설계 전용).

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — adversarial functional-design review (Unit U1 Contracts & Kernel, kind=spec; scope: domain-entities.md + business-rules.md against the six consumed inception contracts)

Verdict: READY

I walked in assuming the enum sets were wrong, the CC-1 mapping diverged, and U1 had either grabbed a domain rule it doesn't own or abandoned one it does. I could not sustain any of those into a blocking finding. Every contract claim resolves to a consumed artifact, and the ownership line is drawn deliberately.

### Blocking (none)

None.

### Verification evidence (what I checked, and why it passed)

- **Shared enums — PASS.** `MeetingStatus` (7 values) matches components.md C0 (개설신청/모집중/시작대기/진행중/완료/반려/취소) and the FR2.3 flow one-for-one, including terminal 반려/취소. `CompletionStatus` (NOT_COMPLETED/COMPLETION_CANDIDATE/COMPLETED) matches C0 (미수료/수료후보/수료확정) and the FR7.1 auto-판정→④확정 flow. `Role` (MENTOR/MENTEE/ADMIN) matches C0 and FR1.5's three roles. State/label descriptions each trace to the correct approval point (① FR2.2, 모집확정 FR3.4, ② FR6.1-context, ③ FR7.2[rev-mk], ④ FR7.1).
- **Value objects — PASS.** `ErrorPayload {code,message,details}` matches NFR8 (전역 에러 스키마) and services.md verbatim; `message` 한국어 correctly cites C5. `Pagination` correctly lifts the component-methods.md carry-over ("페이지네이션·정렬은 functional-design/구현에서 구체화") into a global convention. `EmployeeNo` cites FR1.1/FR1.4 for uniqueness and A5 for the deferred format. `Principal {userId, role}` matches component-methods.md `AuthService.validateSession(token) -> Principal{userId, role}` (C1→U2), and U1 correctly pins only the shape.
- **Error↔HTTP mapping (CC-1) — PASS.** BR-U1-1's table (400/401/403/404/409) matches component-methods.md CC-1 exactly, including "불법 상태 전이 = 항상 409" (components.md "불법 전이는 409") and duplicate/capacity = 409. Global `@RestControllerAdvice` ownership matches component-methods.md.
- **Ownership boundary — PASS.** U1 defines only shared/cross-cutting contracts and explicitly defers: meeting transition table → U3 (components.md C2 owns 전이 집행); 80% completion rule `a*100≥80*S` → U5 (matches component-methods.md `CompletionService.computeCompletion`); 선착순/정원/중복 → U4; 첨부 형식·크기 (A1/OQ4) → U6; messaging/survey → U7/U8. The "규칙 소유 경계" block in business-rules.md is an explicit, correct non-ownership list. Sabun uniqueness enforcement is correctly placed at U2 (DB unique) with U1 owning only the invariant.
- **Epistemic status — PASS.** 사번 형식, 단일 역할, 타임존(KST), enum 물리 표현, 세션 저장 방식 all remain tagged `[assumption]`/`[open]` in both files. None is silently promoted; each names its owning/resolving unit. A4/OQ5 (히든 IP 폐기) is honored.
- **DB baseline & ADR citations — PASS.** Flyway = ADR-003 is confirmed by the unit-of-work.md U1 note ("ADR-003(Flyway)"); baseline common columns (identity PK, timestamptz created_at/updated_at, snake_case per NFR8) are consistent with services.md and team-practices as carried through the consumed contracts.
- **Parallel-build sufficiency — PASS.** The set (enum value sets, ErrorPayload shape, CC-1 mapping, Pagination wrapper, Principal shape, auth header, DB common-column/naming baseline) covers what U2–U9 must compile against. The design-only workflow makes the prose-level #1 OpenAPI scaffold an acceptable deliverable for this stage.
- **Sensors — PASS.** required-sections: domain-entities.md has 7 H2 headings, business-rules.md has 9 (≥2). upstream-coverage: both files' header source-comments and visible prose reference all six consumed artifacts (unit-of-work, unit-of-work-story-map, requirements, components, component-methods, services). No fenced TS/JS/TSX snippets, so linter/type-check have nothing to flag.

### Suggestions (non-blocking)

- **S1 — Pin an error-`code` naming convention.** BR-U1-1/ErrorPayload fix the shape and HTTP mapping but leave `code` values to owning units with only examples (MEETING_INVALID_TRANSITION, ENROLLMENT_FULL, DUPLICATE_EMPLOYEE_NO, INVALID_SORT_FIELD). Since the FE single API client branches on `code` as a stable identifier, a one-line convention (e.g. UPPER_SNAKE `<DOMAIN>_<REASON>`) in U1 would keep parallel units collision-free and FE-branchable. The shape/mapping being pinned is why this is not blocking.
- **S2 — `COMPLETED` literal reused across two enums.** MeetingStatus.COMPLETED (완료) and CompletionStatus.COMPLETED (수료확정) both serialize to the string `"COMPLETED"`. Type-namespaced so it is safe, but a distinct literal for completion (e.g. CERTIFIED) would reduce ambiguity in logs and mixed payloads.
- **S3 — "무상태 검증" slightly prejudges an [open] item.** The 인증·세션 규약 asserts stateless verification while deferring server-session-vs-JWT to U2; a server session is a stateful lookup. It is inherited verbatim from services.md, so not a defect, but consider softening to avoid appearing to pre-decide the deferred U2 choice.
- **S4 — Terminal-state set vs U3 ownership.** BR-U1-3 declares {REJECTED, CANCELLED, COMPLETED} terminal while deferring the transition table to U3. It aligns with FR2.3, but "which states are terminal" is arguably a state-machine property; keep U1's set authoritative and have U3 reference (not redefine) it to prevent divergence.

Verdict: READY
