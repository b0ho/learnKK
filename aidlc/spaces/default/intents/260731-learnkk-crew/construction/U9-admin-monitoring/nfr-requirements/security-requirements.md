# Security Requirements — U9 Admin/Monitoring (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U9 Admin/Monitoring(service, read 계층). 출처: business-logic-model.md(관리자 조회), business-rules.md(BR-U9-1 관리자 전용), requirements.md(NFR6·NFR8·FR9.1). U1 보안 계약 상속. 관리자 전용 접근이 핵심. -->

## 개요

U9는 전체 운영 현황을 보는 **관리자 전용** 조회 계층이라 인가 경계가 핵심. 소유 데이터가 없어 write 공격면은 없음. U1 cross-cutting 상속.

## 인가 (핵심)

- **모든 U9 조회(getApprovalQueues/getMonitoring):** `Principal.role == ADMIN`만 → 아니면 403(BR-U9-1). 멘토·멘티 접근 불가.
- 승인 큐 항목의 실제 액션은 소유 Service(U3/U5)가 각자 ADMIN 재검증 — U9 조회 통과가 액션 인가를 대체하지 않음(이중 방어).

## 데이터 보호

- U9는 전 모임 현황을 관리자에게 노출하므로, **관리자 인가가 전체 데이터 노출의 단일 관문**. 403 게이트 엄격.
- write 없음 → 위변조·주입 공격면 최소. 조합 read는 소유 Unit이 각자 데이터 검증(U9는 표시).
- 조회 결과에 민감정보(비밀번호 등) 미포함 — 큐/모니터링 최소 필드만.

## STRIDE (U9 초점)

| STRIDE | U9 대응 |
|--------|---------|
| Elevation of Privilege | ADMIN 전용 게이트(전체 현황 노출 관문) |
| Info Disclosure | 관리자 외 403, 최소 필드 |
| Tampering | write 없음(read 계층), 액션은 소유 Unit 재검증 |
| Spoofing | 세션 Principal(U2) 전제 |

## 컴플라이언스

- 외부 규제 미적용(C2). 운영 현황=조직 내부.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 멘토/멘티의 승인 큐·모니터링 접근 → 403. 비인증 접근 → 401. 큐 항목 액션은 소유 Service에서 ADMIN 재검증(비ADMIN → 403).

## Assumptions & Open Questions

- **[decided]** 관리자 전용(전체 현황 관문), U9 write 없음, 액션 소유 Unit 재검증.
- **[assumption]** 큐/모니터링 최소 필드(민감정보 배제).
- 운영 TLS·정식 감사는 범위 밖.
## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U9 Admin/Monitoring, kind=service / read-only 조회 계층). 검토 범위 = U9 nfr 5종(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + U1 상속(security-requirements.md). 반증 시도 후 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정(참조 미해소·순환·인가 누수·근거 없는 성능 목표·인식적 승격)으로 요구 ID와 계약을 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 U9 read 계층을 구현하는 능력을 무너뜨리는 근거를 세우지 못함.

### 검증 근거 (Verification evidence — 무엇을 확인했고 왜 통과했나)

- **인가 경계 — PASS.** 모든 U9 조회 `Principal.role == ADMIN` else 403은 functional-design BR-U9-1을 정확히 승계하고, U1 BR-U1-5(role != ADMIN → 403)·CC-1 에러 계약과 정합. 큐 항목 실제 액션의 소유 Service(U3/U5) ADMIN 재검증("조회 통과가 액션 인가를 대체하지 않음")은 이중 방어로, business-logic-model의 액션 위임(approveCreation/confirmRecruitment/approveStart/completeMeeting→U3, approveMenteeCompletion→U5)과 일치. 검증 시나리오의 401(비인증)은 U1 무인증 401 계약과 정합.
- **write 공격면 부재 — PASS.** U9는 세 산출물 전체에서 write가 없다(read-only 조합). Tampering/Injection 대응을 "write 없음 + 소유 Unit 재검증"으로 축소한 것은 read 계층의 실제 위협면과 부합. 소유 데이터가 없어 신규 저장·인가 세부 결정이 없다는 tech-stack TD-U9-3와 일관.
- **전체 현황 노출 관문 — PASS.** "관리자 인가가 전체 데이터 노출의 단일 관문"은 read 계층의 핵심 리스크를 정확히 식별. 최소 필드(민감정보·비밀번호 배제)는 U1 데이터 보호(에러 비노출·목적 한정)와 모순 없음.
- **read 조합 성능 목표 — PASS(견고).** getApprovalQueues/getMonitoring < 2초는 NFR3(체감 1~2초 가이드, 엄격 SLA 아님)에 정합. N+1 회피(배치 조합)를 명시하되, TD-U9-1의 **in-process** Service read + NFR2 파일럿 규모(모임 수십)라 배치가 미성사되어도 프로세스 내부 호출 수십 회는 <2초를 위협하지 않음 — 목표가 scale로 방어됨. 관리자 저빈도 전제도 스파이크 부재를 뒷받침.
- **확장 경계 — PASS.** 단일 JVM·단일 DB(NFR4)·수평 확장 범위 밖. U9 자체 저장 없음 → 데이터 증가 요인 아님, 부하는 소스 Unit(U3/U4/U5) 조회로 위임. 대량 모임 시 페이지네이션·집계 사전계산은 FR9.2 TBD로 범위 밖 처리 — 정확.
- **신뢰성/부분 실패 — PASS.** 무상태 조회·캐시 없음 → stale 없음(TD-U9-3와 일관), 집계 미저장 → 집계 불일치 리스크 없음(BR-U9-4). graceful degradation(성공 큐 표시·실패 큐 오류 배지)은 business-logic-model 에러·엣지 케이스와 일치. SLA/SLO 없음은 NFR4 파일럿 전제와 정합.
- **과다설계 배제 — PASS.** BI 도구·대시보드 캐시·집계 지표 사전계산을 tech-stack 범위 밖과 scalability에서 FR9.2(TBD)·US-9.3(Won't) 인용으로 정확히 제외. read 계층에 불필요한 저장/캐시 도입 없음.
- **U1 상속 무모순 — PASS.** CC-1 에러 계약, RBAC 403, 에러 비노출(민감정보 미포함), 시크릿 비커밋·정적분석·의존성 스캔 상속 — U1 security-requirements와 충돌 없음. 외부 규제 미적용(C2)도 U1 컴플라이언스 논리와 동일.
- **인식적 상태(Epistemic) — PASS(가장 날카로운 검사).** U9가 필요로 하는 4개 read 포트(U3 listByStatus, U5 listByCompletion, U5 allScheduledSessionsEnded, U4 count)는 tech-stack·performance·scalability에서 일관되게 `[open]`으로 유지 — functional-design 검토가 확인한 대로 이들은 아직 component-methods.md에 부재하며, NFR 산출물 어디서도 `[decided]`로 암묵 승격되지 않음. `[decided]`(관리자 전용·write 없음·in-process 조합·캐시 없음)와 `[assumption]`(배치 조합·최소 필드)·`[open]`(포트 시그니처·집계 지표)의 경계가 유지됨.
- **순환 의존성 — PASS.** U9 read는 U3/U4/U5로 향하는 DAG 정방향(U9 최상위)이며 어느 Unit도 U9를 read하지 않음(read-out 없음) → 조합이 순환을 닫을 수 없음. BR-U9-5(직접 테이블 접근 금지, Service read만)와 모듈러 모놀리스 계약에 정합. (선언된 U8 의존은 이번 범위 미사용 — functional-design에서 향후 피드백-뷰 예약으로 정리됨, 순환 아님.)
- **센서 — PASS.** required-sections(≥2 H2): performance 4, security 7, scalability 4, reliability 6, tech-stack 4 — 전부 충족. produces_kinds상 service Unit에 5종 전부 적용 대상이며 5종 모두 존재. upstream-coverage: 5개 파일 모두 business-logic-model·business-rules·requirements를 헤더·prose에서 참조(BR-U9-*·NFR2/3/4/5/6/8·FR9.1/9.2 인용). 코드 펜스(TS/JS/TSX) 없음 → linter/type-check 대상 없음.

### Suggestions (non-blocking)

- **S1 — 부분 실패 시 HTTP 상태 의미를 한 줄 고정 권고.** reliability는 "read 실패는 5xx 명시적(silent 금지)"과 "부분 실패 시 성공 큐 표시·실패 큐 오류 배지"(암묵 200)를 동시에 기술한다. 5개 큐 중 2개 실패 시 200(행별 오류)인지 5xx인지 개발자가 판단해야 한다. 이미 `[open]`("부분 실패 표시 단위 상세는 code-generation 전 확정")으로 게이트되어 있고 "전체 실패=5xx / 부분=200+행 오류"라는 합리적 기본값으로 구현 가능하므로 blocking 아님. code-generation 전 이 이분법을 한 줄 명시하면 인식적 완결성이 오른다.
- **S2 — 배치 조합(TD-U9-2)은 소유 Unit read 포트의 배치 수용을 전제.** N+1 회피는 U4/U5가 id-목록 기반 배치 read를 노출해야 성립하나 해당 시그니처는 `[open]`이다. 포트가 per-id만 노출하면 배치가 무산된다 — 다만 in-process·파일럿 규모라 <2초는 여전히 방어됨(위 성능 근거). U3/U4/U5 계약 확정 시 배치 read 노출 여부를 명시적으로 넘겨받길 권고. (이미 `[open]`으로 태깅됨 → 이월 불요.)
- **S3 — 모니터링 조합의 U3 read 명시.** performance의 getMonitoring 근거는 "모임 목록 × U4/U5 read 조합"으로 표기되나, getMonitoring은 상태·정원(capacity)을 U3에서 읽는다(business-logic-model W2). 근거 열의 축약 표기라 결함은 아니나, 정원=U3 소유(functional-design 검토 S1과 동일 귀속)임을 감안해 "U3(상태·정원)×U4(신청 수)×U5(출석·수료)"로 적으면 소유 경계 표기가 정확해진다. 비blocking.

Verdict: READY
