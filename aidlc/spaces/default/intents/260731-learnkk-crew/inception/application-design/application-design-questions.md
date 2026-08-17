# Application Design — 인터뷰 질문지 (learnKK / 런크크)

team-practices가 이미 고정한 것(monorepo, 3계층 Controller/Service/Repository, Entity 비노출·DTO 경계, JSON camelCase/JPA snake_case, 전역 에러 스키마, 3계약 #1 OpenAPI/#2 DB/#3 도메인 타입)과 requirements/stories(rev4까지)를 전제로, 아키텍처 결정만 확정합니다.

각 `[Answer]:`에 보기 문자. 복수 선택 문항 표시. 직접 서술은 `X. 기타`.

---

## Q1. 백엔드 아키텍처 스타일 (전부 로컬·파일럿 규모)
- A. (권장) **모듈러 모놀리스** — 단일 Spring Boot 앱 안에서 도메인 모듈(패키지) 경계로 분리. 로컬 단일 인스턴스·소규모에 적합, 3인 병렬은 모듈 소유로 배분.
- B. 단순 계층형 모놀리스(도메인 모듈 경계 약하게)
- C. 마이크로서비스(다중 프로세스) — 로컬·소규모엔 과함
- X. 기타

[Answer]:a

## Q2. 도메인(컴포넌트) 분해
아래 도메인 모듈로 나누는 안을 제안합니다. 조정할까요? (해당되는 것 모두/기타)
- A. (권장) **7개 도메인 모듈**: ①Auth/User(사번·인증·프로필) ②Meeting(개설·승인·상태머신) ③Enrollment(신청·모집확정) ④Session/Attendance(세션 일정·팝업 출석·출석율) ⑤Content(게시글·첨부·공지) ⑥Messaging(쪽지) ⑦Survey/Feedback(사전설문·과정설문). Admin 승인은 각 도메인의 상태전이 API로 흡수 + 공통 Approval 조회.
- B. 더 굵게(3~4개로 통합)
- C. 더 잘게(승인·수료를 별도 모듈로)
- X. 기타

[Answer]:a

## Q3. 서비스 간 통신 / 호출 방식
- A. (권장) **인-프로세스 동기 호출**(모듈 간 Service 인터페이스 직접 호출) + 클라이언트↔서버는 **동기 REST(JSON)**. 로컬 단일 인스턴스라 메시지 브로커/이벤트버스 불필요.
- B. 모듈 간에도 도메인 이벤트(인메모리 이벤트) 도입
- C. 비동기 큐/브로커 도입 — 범위 밖(전부 로컬)
- X. 기타

[Answer]:a

## Q4. 데이터 소유·저장 전략
- A. (권장) **단일 PostgreSQL** + 도메인 모듈별 스키마/테이블 소유(모듈이 자기 테이블만 소유, 교차 접근은 Service 경유). 첨부는 BLOB(stories US-4.1b). 마이그레이션 도구는 구현 워크플로우 확정(Flyway/Liquibase 후보).
- B. 단일 DB + 공유 접근(소유 경계 약하게)
- C. 모듈별 분리 DB — 범위 밖
- X. 기타

[Answer]:a, flyway

## Q5. 세션·출석 시간 처리 (rev2 세션 일정)
멘토 세션 일정(날짜·시간) + 예정 시간 팝업 출석은 시간 기반입니다.
- A. (권장) **스케줄러/배치 없이** 요청 시점에 현재 시각 vs 세션 예정 시각을 비교해 출석 창 유효성 판정(로컬 단일 인스턴스, developer 기고). 출석 유효 시간창 파라미터는 functional-design 확정.
- B. 경량 스케줄러(앱 내 @Scheduled)로 세션 상태 갱신
- X. 기타

[Answer]:a

## Q6. 프론트엔드 구조 (React + shadcn/ui)
- A. (권장) **기능(feature) 기반 폴더 구조** + 단일 **API client 계층**(team-practices 경계 규약) + 상태관리는 경량(서버 상태는 fetch 캐시 계층, 전역 UI 상태 최소). 라우팅은 3탭 + 화면.
- B. 도메인 기반 폴더 + 무거운 전역 상태관리(Redux 등)
- X. 기타 (상태관리 라이브러리 지정 등)

[Answer]:a

## Q7. ADR로 남길 핵심 결정 (decisions.md)
어떤 결정을 ADR로 기록할까요? (해당되는 것 모두 선택 가능)
- A. (권장) 모듈러 모놀리스 채택 / 동기 REST+인-프로세스 / 단일 PostgreSQL·모듈 소유 / 첨부 BLOB 저장 / 스케줄러리스 시간 판정 / 상태머신(#3 계약) 소유 위치
- B. 위 중 일부만 (번호 지정)
- X. 기타

[Answer]:a

---

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation

정리된 답변:
- Q1 = A — 모듈러 모놀리스(단일 Spring Boot, 도메인 모듈 경계, 3인 모듈 소유 배분).
- Q2 = A — 7개 도메인 모듈(Auth/User, Meeting, Enrollment, Session/Attendance, Content, Messaging, Survey/Feedback) + 공통 Approval 조회.
- Q3 = A — 인-프로세스 동기 Service 호출 + 클라이언트↔서버 동기 REST(JSON). 브로커/이벤트버스 없음.
- Q4 = A + **Flyway** — 단일 PostgreSQL·모듈별 테이블 소유·교차 접근은 Service 경유, 첨부 BLOB, 마이그레이션 **Flyway 확정**.
- Q5 = A — 스케줄러리스, 요청 시점 현재 시각 vs 세션 예정 시각 비교로 출석 창 판정.
- Q6 = A — feature 기반 폴더 + 단일 API client 계층 + 경량 상태관리(서버상태 fetch 캐시).
- Q7 = A — 핵심 결정 전부 ADR화(모듈러 모놀리스·동기 REST·단일 PostgreSQL·BLOB·스케줄러리스·상태머신 소유).

프롬프트: "이대로 application-design 산출물(components/component-methods/services/component-dependency/decisions)을 생성해도 될까요?"
- A. Looks correct — 생성 진행
- B. Request changes — 일부 답변 수정

[Answer]: A. Looks correct

---

<!-- §13 Learnings Ritual — pending human turn (blank [Answer] marks genuine human-wait for the Stop hook) -->
## Learnings Ritual
프롬프트: "surface된 후보(c1~c2) 중 harness에 남길 항목을 고르고, 다음을 위해 추가할 메모가 있습니까?"
후보: c1(전부 로컬이라 aws 관점 최소·team-practices 위에서 구체화), c2(상태/수료 enum은 shared kernel 소유·전이 집행은 단일 모듈로 몰아 interface 불일치 차단) — 각 `→ project.md`, team.md 승격 가능.
(참고: 리뷰어 B1 교훈 "전제조건 집행은 그 상태 소유 모듈에 대한 read 의존을 반드시 명시"는 memory에 open-question으로 기록됨 — 남길 만한 durable 원칙.)
- 1. 아무것도 남기지 않음
- 2. 후보 선택 (남길 번호 지정; team 승격 여부)
- 3. 메모 추가 (자유 서술 + diary 헤딩 선택)

[Answer]: 1. 아무것도 남기지 않음
