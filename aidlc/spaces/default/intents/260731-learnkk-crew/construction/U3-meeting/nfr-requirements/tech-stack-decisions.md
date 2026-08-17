# Tech Stack Decisions — U3 Meeting (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U3 Meeting(service). 출처: business-logic-model.md(전이·문항·허브), business-rules.md(BR-U3-1 조건부 UPDATE), requirements.md(C1 스택·NFR8). U1 tech-stack(스택·계약 도구·enum varchar+CHECK) 상속. U3는 상태머신 구현 기술 선택. -->

## 개요

U1 스택·계약 도구 상속(React+Spring+PostgreSQL, OpenAPI/Flyway/enum varchar+CHECK). U3는 상태머신·문항 빌더의 구체 기술 선택을 확정.

## U3 기술 선택

### TD-U3-1. 상태 전이 동시성 — 조건부 UPDATE (또는 낙관적 락)

- **결정:** `UPDATE meeting SET status=? WHERE id=? AND status=<expected>` — affected rows=0이면 409. 상태 컬럼 기반 낙관적 동시성.
- **근거:** 이중 승인 경합을 DB 원자성으로 직렬화(애플리케이션 락·분산락 불요). 단일 인스턴스라 충분.
- **대안:** `@Version` 낙관적 락 — 동등 효과, status 조건부 UPDATE가 더 명시적.

### TD-U3-2. 상태머신 구현 — 명시적 전이표(코드)

- **결정:** 전이표(BR-U3-1)를 Service 계층 코드로 집행(별도 워크플로우 엔진 미도입). MeetingStatus enum(U1)을 varchar+CHECK로 저장.
- **근거:** 6개 전이의 소규모 상태머신 — 프레임워크 오버킬. 3계층 Service에 자연 배치(ADR-006 상태머신 단일 소유).

### TD-U3-3. 문항 빌더 저장 — 정규화 테이블

- **결정:** `survey_question` 테이블(meeting FK, orderNo, type, options). 선택 보기는 `text[]`/json [assumption].
- **근거:** 문항 순서·유형 질의 용이. 응답은 U8 소유.

### TD-U3-4. 운영 허브 조합 — FE 단일 API client

- **결정:** 운영 허브는 백엔드 조인 아닌 **FE 병렬 호출 조합**(U3/U4/U8 엔드포인트). U2가 셋업한 단일 API client 재사용.
- **근거:** 백엔드 교차-Unit 의존(순환) 회피(business-logic-model W4).

## 범위 밖

- 워크플로우 엔진·이벤트 소싱(오버킬), CI/CD·운영(C3).

## Assumptions & Open Questions

- **[assumption]** 문항 보기 저장 형태(text[]/json), 조건부 UPDATE vs @Version.
- **[open]** U4/U5 read 포트 시그니처(U4/U5 functional-design).
