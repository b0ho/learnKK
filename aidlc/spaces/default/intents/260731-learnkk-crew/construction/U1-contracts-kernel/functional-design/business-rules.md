# Business Rules — U1 Contracts & Kernel (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U1 Contracts&Kernel(kind=spec). 출처: unit-of-work.md(U1 계약 소유·선고정), unit-of-work-story-map.md(U1 스토리 비소유), requirements.md(FR1.4·FR2.3·FR7.1·NFR8·CC-1 규약·C5), components.md(C0 shared kernel·상태머신 enum은 C0/집행은 C2), component-methods.md(공통 에러 처리 CC-1·전역 @RestControllerAdvice), services.md(전역 에러 {code,message,details}·REST 규약). U1 business-rules = 전 Unit이 상속하는 교차 관심(cross-cutting) 규칙: 에러 코드/상태 매핑, 사번 유일성, 상태 enum 값 불변식, camelCase 경계 규약. 도메인별 전이 규칙 자체는 소유 Unit(U3/U5)에 위임. -->

## 개요

U1은 전 Unit이 상속하는 **cross-cutting 규칙**을 확정한다. 특정 도메인의 전이 조건(모임 상태 전이, 출석 판정 등)은 소유 Unit(U3, U5)의 business-rules가 정의하며, 여기서는 그 규칙들이 공통으로 따르는 **계약 규약**(에러 매핑·네이밍·유일성·불변식)을 고정한다.

## BR-U1-1. 전역 에러 코드 ↔ HTTP 상태 매핑 (CC-1)

component-methods.md 공통 에러 처리·stories CC-1을 정본화. 전 Unit의 REST 응답은 이 매핑을 따르고, 본문은 `ErrorPayload {code,message,details}`(domain-entities.md).

| HTTP | 의미 | 대표 상황 |
|------|------|-----------|
| 400 | 검증 실패 | 필수값 누락·형식 오류·비즈니스 검증 실패 |
| 401 | 인증 실패 | 무효/만료 세션, 로그인 실패(계정 존재 비특정) |
| 403 | 인가 실패 | 역할·소유 경계 위반(비참여자 열람 등) |
| 404 | 미존재 | 대상 리소스 없음 |
| 409 | 상태 충돌 | 불법 상태 전이, 중복(사번/신청), 정원 마감 |

- 규칙: 상태머신 불법 전이는 **항상 409**(stories CC-1). 중복 가입·중복 신청도 409. 정원 마감은 409(또는 도메인 코드 `ENROLLMENT_FULL`).
- **에러 `code` 네이밍 규약(전 Unit 상속):** `UPPER_SNAKE_CASE`, 형식 `<DOMAIN>_<REASON>`(예: `MEETING_INVALID_TRANSITION`, `ENROLLMENT_FULL`, `DUPLICATE_EMPLOYEE_NO`, `INVALID_SORT_FIELD`). DOMAIN 접두어로 Unit 간 코드 충돌을 방지하고, FE 단일 API client가 `code`로 안정 분기한다.
- 구현 규약: 전역 `@RestControllerAdvice`가 도메인 예외를 이 매핑으로 변환(team-practices, component-methods.md). 개별 컨트롤러는 상태코드를 직접 조립하지 않는다.
- `message`는 한국어(requirements C5). `code`는 영문 안정 식별자.

## BR-U1-2. 사번 유일성 (FR1.4 / 중복계정 방지)

- **불변식:** 하나의 `EmployeeNo`당 계정은 정확히 1개. 정규화(대문자·공백제거) 후 비교하여 유일성 판정.
- **집행 위치:** 쓰기 무결성은 U2 Auth의 DB unique 제약(사번 컬럼)이 최종 보증. 애플리케이션 선검증은 사용자 친화 메시지용(중복 시 409, `code=DUPLICATE_EMPLOYEE_NO`).
- **경합:** 동시 가입으로 unique 제약 위반 시 애플리케이션은 이를 409로 변환(경합-안전). 히든 IP 신호 방식은 폐기(requirements A4/OQ5).
- **형식 [assumption]:** domain-entities.md 참조(영숫자 4~20자, 파일럿 가정). 형식 위반은 400.

## BR-U1-3. 상태 enum 값 불변식 (#3 계약)

- `MeetingStatus`·`CompletionStatus`·`Role`의 **값 집합은 U1이 단일 소유**. 어느 Unit도 새 상태 값을 임의 추가하지 않는다(계약 변경은 U1 마이그레이션+합의를 거침).
- 종료 상태(`REJECTED`, `CANCELLED`, `COMPLETED`)에서의 재전이는 불가 — 위반 시 409. (구체 전이표는 U3 소관, 여기서는 "종료 상태 불변" 원칙만.)
- `CompletionStatus`는 멘티×모임 단위로 유지되며 `NOT_COMPLETED`가 기본값. (판정 규칙은 U5.)

## BR-U1-4. API 경계 네이밍·직렬화 규약 (NFR8)

- JSON 필드는 **camelCase**, JPA 물리 컬럼은 **snake_case**. 경계 변환은 직렬화 계층이 담당.
- **Entity를 API 경계에 노출 금지** — Controller는 `XxxRequest`/`XxxResponse` DTO만 주고받는다(team-practices, components.md).
- 도메인 enum은 JSON에서 **enum 명 문자열**로 직렬화(정수 코드 금지) — FE 가독성·안정성.
- 날짜·시각은 ISO-8601(`timestamptz`), 타임존 포함. 응답은 UTC 또는 서버 로컬 명시(구현에서 단일 규약 고정 — 파일럿은 서버 로컬 KST 가정 [assumption]).

## BR-U1-5. 인가 판단 공통 규약

- 모든 보호 엔드포인트는 `Principal {userId, role}`을 요구. 무인증 접근은 401.
- 역할 게이트(예: 관리자 전용 승인 액션)는 `role != ADMIN` 시 403.
- 소유·참여 경계(예: 자기 모임 멘티만, 참여자만 열람)는 도메인 Unit이 세부 판정하되, 위반 시 **403**으로 통일(BR-U1-1).

## BR-U1-6. 페이지네이션·정렬 규약

- 목록 응답은 domain-entities.md Pagination 래퍼를 따른다. `size` 상한 초과 요청은 상한으로 clamp(400 아님) — 파일럿 관용.
- 정렬 파라미터가 허용되지 않은 필드를 지정하면 400(`code=INVALID_SORT_FIELD`).

## 규칙 소유 경계 (이 Unit이 정의하지 않는 것)

명시적으로 **U1 범위 밖**(소유 Unit에서 정의):

- 모임 상태 전이표·전이 전제조건(①/모집확정/②/③·반려/취소) → U3 Meeting.
- 선착순·정원·중복 신청 경계 → U4 Enrollment.
- 출석 유효 시간창·출석율·80% 판정(a*100≥80*S) → U5 Session/Attendance.
- 첨부 형식 화이트리스트·크기 상한(A1/OQ4) → U6 Content.
- 쪽지 권한 경계 세부 → U7 Messaging.
- 사전설문 ②후 게이팅 → U8 Survey/Feedback.

U1은 위 규칙들이 공통으로 사용하는 에러 코드·상태 값·네이밍·인가 규약만 고정한다.

## Assumptions & Open Questions

- **[assumption]** 사번 형식(영숫자 4~20자)·타임존(서버 로컬 KST)·단일 역할은 파일럿 가정 — 조직 확정 시 교체(requirements A5 유지).
- **[open]** enum 물리 표현(varchar+CHECK vs PG enum 타입) 최종 선택은 구현 워크플로우.
- **[open]** 세션 저장 방식(서버 세션 vs JWT)은 U2에서 확정 — 인가 규약(Principal)은 방식 무관.
- 이 문서는 계약 규칙 정의이며 실제 구현·코드는 이번 워크플로우 범위 밖(설계 전용).
