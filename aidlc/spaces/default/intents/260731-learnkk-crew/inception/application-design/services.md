# Services — learnKK (런크크)

<!-- application-design 산출물. 배포 가능한 프로세스(service) 관점 + 오케스트레이션. 전부 로컬 단일 인스턴스, 모듈러 모놀리스라 런타임 서비스는 소수. 출처: components.md, requirements(NFR), team-practices. -->

## 런타임 서비스(프로세스)

모듈러 모놀리스이므로 도메인 모듈(C1~C8)은 **별도 프로세스가 아니라 단일 백엔드 프로세스 내 모듈**이다. 배포 단위(process/container) 관점의 서비스는 다음 3개다.

### S1. Frontend (React SPA)
- **책임:** 모바일 웹뷰 UI(3탭·화면), 단일 API client로 백엔드 REST 호출.
- **런타임:** 로컬 dev server(빌드 산출물은 정적). 로컬 단일 인스턴스.
- **스케일:** 단일(파일럿). 클라이언트 렌더.

### S2. Backend (Spring Boot 모놀리스)
- **책임:** C0~C8 도메인 모듈 전체를 호스팅. REST API 제공, 3계층, 인증/세션, 비즈니스 로직, 데이터 접근.
- **런타임:** 단일 JVM 프로세스(내장 톰캣). 로컬. 인-프로세스 모듈 간 동기 호출.
- **스케일:** 단일 인스턴스(NFR2/NFR4). HA·다중화 범위 밖.

### S3. PostgreSQL (데이터 저장)
- **책임:** 단일 DB. 모듈별 테이블 소유(논리적), 첨부 BLOB. Flyway 마이그레이션.
- **런타임:** 로컬(개발자 머신 또는 docker-compose, team-practices Deployment).
- **스케일:** 단일 인스턴스. 백업 자동화 범위 밖.

로컬 실행은 team-practices대로 **docker-compose로 표준화**(PostgreSQL 등), 시크릿 비커밋(`.env`/Spring profile). CI/CD·배포는 후속 구현 워크플로우.

## 통신 계약

- **클라이언트↔백엔드:** 동기 **REST(JSON)**, OpenAPI 계약(#1)으로 고정. JSON camelCase, 전역 에러 `{code,message,details}`. 인증은 세션 토큰(헤더). [Q3]
- **모듈↔모듈(백엔드 내부):** 인-프로세스 **동기 Service 인터페이스 호출**. 메시지 브로커·이벤트버스 없음(로컬·소규모). [Q3]
- **백엔드↔DB:** JPA/Repository, snake_case 물리 네이밍. 첨부는 BLOB 스트리밍(OOM 회피).

## 오케스트레이션 패턴

- **오케스트레이션(orchestration) 중심** — 모듈 간 협력이 필요한 흐름은 호출 측 Service가 순서를 조율(choreography/이벤트 아님).
- 대표 흐름:
  - **신청→모집확정→시작:** Enrollment(C3) 신청 → MeetingApproval(C2) 모집확정/②시작. 상태 전이는 C2가 단일 집행.
  - **세션→출석→수료:** Session(C4) 일정 → Attendance(C4) 팝업 체크 → Completion(C4) 자동 판정 → MeetingApproval/Completion 관리자 ④승인.
  - **완료 처리(③):** 전 세션 종료 → 관리자가 MeetingApproval.completeMeeting 직접 호출(rev3, 멘토 신청 없음).
  - **관리자 조회:** AdminQuery(C8)가 C2/C4 등 read 조합(승인 큐·현황).

## 서비스 수준·비기능 (NFR 매핑)

- **성능(NFR3, 가이드):** 화면/목록 응답 체감 1~2초 — 인-프로세스·단일 DB로 파일럿 규모 충족 목표(엄격 SLA 아님, 검증은 performance-validation).
- **규모(NFR2):** 동시 수십 명·모임 수십 개. 단일 인스턴스로 충분 설계.
- **가용성(NFR4):** 단일 인스턴스 전제. 장애 복구·HA는 범위 밖.
- **보안(NFR6/8):** 세션 인증·RBAC(C1), bcrypt, 시크릿 비커밋, 보안 정적분석·의존성 스캔(team-practices). 전부 로컬.
- **라이프사이클:** 무상태 세션 검증(토큰), 세션 저장은 DB. 배치/스케줄러 없음(Q5 — 시간 판정은 요청 시점).

## Assumptions & Open Questions

- docker-compose 구성·프로파일·시크릿 주입 상세는 구현 워크플로우(team-practices Deployment).
- 세션 저장 방식(DB 세션 vs JWT)은 functional-design/구현에서 확정 — 단일 인스턴스라 서버 세션도 무리 없음.
