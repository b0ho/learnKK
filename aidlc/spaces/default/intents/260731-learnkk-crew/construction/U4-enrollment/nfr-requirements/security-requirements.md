# Security Requirements — U4 Enrollment (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U4 Enrollment(service). 출처: business-logic-model.md(신청·취소·현황), business-rules.md(BR-U4-1~5 인가), requirements.md(NFR6·NFR8·FR3.2/3.5). U1 보안 계약 상속·집행. -->

## 개요

U4는 신청 접수의 인가 경계(멘티 본인)와 정원 무결성을 보안 관점에서 지킨다. U1 cross-cutting 보안 상속.

## 인가 (핵심)

- **신청·취소:** role=MENTEE + 본인(menteeId==Principal.userId). 위반 403(BR-U4-5). 타인 대신 신청/취소 불가.
- **신청자 목록:** 소유 멘토/관리자만(403 경계). 멘티는 타인 신청자 목록 열람 불가.
- **멘티 현황:** 본인 신청만.

## 데이터 무결성 (보안 관점)

- **정원 무결성(overbooking 금지):** 동시성 락으로 활성 신청 수 ≤ capacity 보장(BR-U4-1). 이는 가용성·공정성(선착순) 보안 속성.
- **중복 방지:** unique(meeting,mentee)로 한 멘티 다중 신청 차단(BR-U4-2).
- 입력 검증 400. 신청 위조(타 멘티 사칭)는 Principal 기반 본인 확인으로 차단.

## STRIDE (U4 초점)

| STRIDE | U4 대응 |
|--------|---------|
| Tampering | 정원/중복 무결성(락·unique), 서버 권위 |
| Spoofing | 본인(menteeId==Principal) 확인 |
| Info Disclosure | 신청자 목록 소유 경계 403 |
| Elevation of Privilege | 멘티 전용 액션, 관리자/멘토 신청 불가 |
| DoS | 정원 폭주는 파일럿 규모 밖(락 경합 경미) |

## 컴플라이언스

- 외부 규제 미적용(C2). 신청 데이터=조직 내부.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 타 멘티 사칭 신청/취소 → 403. 멘티의 신청자 목록 열람 → 403. 잔여 1석 2인 → 1인 성공/1인 409. 중복 신청 → 409. ②후 취소 → 409.

## Assumptions & Open Questions

- **[assumption]** 재신청 불가·대기열 없음.
- **[open]** 정원 락 방식(무결성 필수).
- 운영 TLS·정식 감사는 범위 밖.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U4 Enrollment, kind=service). 검토 범위 = U4 nfr 5산출물(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model·business-rules·requirements) + U1 상속(security-requirements·tech-stack). 반증 시도(잔여-1석 overbooking 경합, U1 계약 모순, 락 방식의 암묵 승격) 후 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정으로 요구 ID·계약·불변식을 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 이 NFR을 구현 가능한 수준을 무너뜨리는 근거를 세우지 못함.

### 검증 근거 (Verification evidence)

- **overbooking 금지 불변식 (BR-U4-1) — PASS.** 5개 NFR 파일 전부 `활성(APPLIED) 신청 수 ≤ capacity`를 신뢰성·보안의 핵심 불변식으로 일관 서술. 기제는 (a) `pg_advisory_xact_lock(meetingId)` 모임 단위 트랜잭션 락(커밋까지 유지 → 다음 홀더의 count가 직전 insert를 관측, count-then-insert 무경합) 또는 (b) SERIALIZABLE + serialization_failure 재시도/거부. U4 functional-design W1과 BR-U4-1을 정확히 승계. count가 `status=APPLIED`만 세므로 동시 cancel(APPLIED→CANCELLED)은 활성 수를 낮출 뿐 overbooking 유발 불가. 잔여-1석 경합은 1인 APPLIED / 1인 409 ENROLLMENT_FULL로 귀결(security 검증 시나리오·business-logic-model 엣지케이스 일치).
- **U3 소유 위반 회피 — PASS.** tech-stack TD-U4-1이 U3 `meeting` 행 `FOR UPDATE`를 명시적 비채택(모듈 소유 위반)으로 기록하고, 어드바이저리 락(모임 키만 직렬화, 테이블 락 아님) 또는 U4 소유 카운터 행 변형을 대안으로 둠. business-logic-model W1 "U3 meeting 행은 잠그지 않음", business-rules BR-U4-1과 정합. 서로 다른 모임 병렬성도 세 파일(performance·scalability·tech-stack)에서 일관.
- **중복 방지 — PASS.** `unique(meeting_id, mentee_id)` → 409 ENROLLMENT_DUPLICATE(BR-U4-2). security·reliability·tech-stack TD-U4-2 일관. [assumption] 재신청 불가와도 모순 없음(CANCELLED 행이 pair를 점유 유지 → DB 수준 재신청 차단).
- **인가·사칭·정보노출 (devsecops 렌즈) — PASS.** 신청/취소 role=MENTEE + menteeId==Principal.userId 위반 403(BR-U4-5); 신청자 목록 소유 멘토/관리자 경계 403; 정원 무결성을 가용성·공정성(선착순) 보안 속성으로 명시. STRIDE 표가 Tampering(락·unique)·Spoofing(본인 확인)·Info Disclosure(소유 경계 403)·EoP(멘티 전용)·DoS(파일럿 범위 밖)를 U4 초점으로 커버.
- **FE 조합·순환 회피 — PASS.** 멘티 현황의 U5 세션 read를 백엔드 U4→U5 아닌 FE 화면 조합으로(tech-stack TD-U4-3, performance 현황 화면, reliability graceful degradation) 일관 라우팅. business-logic-model W4와 정합, U4↔U5 백엔드 순환 없음.
- **취소 규칙 — PASS.** reliability APPLIED→CANCELLED 원자 전이·취소 행 보존, security "②후 취소 → 409" 검증 시나리오가 BR-U4-3(②전 {RECRUITING, READY_TO_START} 허용, IN_PROGRESS 이후 409 CANCEL_FORBIDDEN)·FR3.5와 정합.
- **U1 상속 — PASS.** 외부 규제 미적용(C2)·시크릿 비커밋·정적분석·의존성 스캔·CC-1 409/403/400/404·ErrorPayload 비노출·RBAC 403이 U1 security-requirements와 무모순 승계. tech-stack이 U1 확정 스택(React+Spring+PostgreSQL/로컬)·OpenAPI/Flyway/enum varchar+CHECK 상속을 인용하고, 어드바이저리 락은 U4-added로 명확히 구분 — U1 TD-1~TD-6 어떤 계약도 재정의·모순하지 않음.
- **NFR 현실성(로컬 파일럿) — PASS.** perf apply<1s(count+insert 짧은 락 구간, NFR3 1~2초 가이드), scalability 단일 인스턴스·모임 단위 락 격리(NFR2 수십·NFR4), reliability SLA 없음·트랜잭션 원자성·롤백(NFR4/NFR5). 분산 락·MQ는 tech-stack 범위 밖으로 명시 배제(오버엔지니어링 회피) — 파일럿 규모에 적정.
- **인식적 상태(Epistemic) — PASS.** 락 방식(어드바이저리 vs SERIALIZABLE), SERIALIZABLE 재시도 정책, 다중 인스턴스 락, U3/U5 read 포트 시그니처는 5파일 전반에서 [open]로 유지; 재신청 불가·대기열 없음·단일 트랜잭션·인덱스는 [assumption]. overbooking 금지 불변식만 필수로 단정(기제 무관하게 두 후보 모두 보장) — 락 방식의 암묵 승격 없음, 모순 아님.
- **센서 — PASS.** required-sections: performance 4 / security 7 / scalability 4 / reliability 6 / tech-stack 4 H2(모두 ≥2). upstream-coverage: 5파일 각각 business-logic-model·business-rules·requirements를 출처 주석 및 본문(BR-U4-x, NFR2/3/4/5, FR3.2/3.5)에서 참조. 펜스드 TS/JS/TSX 코드 없음(의사코드는 plain) → linter/type-check 대상 없음.

### Suggestions (non-blocking; 이월 불요)

- **S1 — STRIDE 표 Repudiation 부재.** U4 STRIDE 표는 "U4 초점"으로 5개 카테고리만 다루고 Repudiation을 생략. U1 cross-cutting STRIDE가 감사 로깅(승인/액션 최소 기록)을 상속 커버하므로 계약 결함은 아니나, 선착순 공정성의 핵심 레코드인 `appliedAt`(신청 순서 비부인)을 한 줄 언급하면 fairness 속성 주장이 더 완결된다.
- **S2 — status-gate TOCTOU의 NFR 계층 상호참조.** business-logic-model 엣지케이스가 이미 `status==RECRUITING` 검사(원자 구간 밖, U3 소유 상태)와 admin `confirmRecruitment` 사이의 좁은 경합 창을 문서화했다. overbooking 불변식은 락으로 무영향이나, reliability-requirements(데이터 무결성 절)에서 이 교차모듈 상태 경합을 한 줄 상호참조하면 NFR 계층에서도 무결성 서술이 완결된다.
- **S3 — 카운터 행 변형의 도메인 엔티티 함의.** TD-U4-1의 "U4 소유 카운터/집계 행 FOR UPDATE" 변형은 현행 count(enrollment) 방식과 달리 신규 소유 테이블을 요구한다. 변형으로 태그되어 있어 비블로킹이나, 채택 시 domain-entities 추가가 필요함을 구현 시 유의.

Verdict: READY
