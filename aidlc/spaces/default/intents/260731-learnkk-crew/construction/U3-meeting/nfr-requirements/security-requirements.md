# Security Requirements — U3 Meeting (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U3 Meeting(service). 출처: business-logic-model.md(전이·운영허브), business-rules.md(BR-U3-1~7·인가), requirements.md(NFR6·NFR8·FR2.2 ①/US-6.1 ②/FR7.2 ③ 관리자 승인). U1 보안 계약 상속·집행. -->

## 개요

U3는 상태 전이·모임 데이터의 보안 경계를 소유. 승인 액션은 ADMIN 전용, 개설은 MENTOR 전용, 조회는 역할·소유 경계. U1 cross-cutting 보안 계약 상속.

## 인가 (핵심)

- **관리자 전이 액션(①/모집확정/②/③):** `Principal.role == ADMIN` 아니면 403(BR-U3-2). 서버 권위.
- **개설·사전설문 템플릿 편집:** role=MENTOR + 소유 멘토(자기 모임)만. 타인 모임 편집 403.
- **운영 허브 조회:** 자기 모임만(소유 경계 403). 신청자·멘티 사전설문 게시글 열람은 소유 멘토/관리자만(U4/U8이 각 403 집행).
- **모집 목록:** RECRUITING 모임은 로그인 사용자에 공개(민감정보 아님).

## 데이터 보호

- 사전설문 템플릿은 모임 참여자·멘토·관리자 범위. 멘티 사전설문 게시글은 U8 소유(권한 U8).
- 반려 사유(rejectReason)는 개설 멘토·관리자에 노출.
- 입력 검증 400(BR-U3-3). 상태 전이 무결성은 조건부 UPDATE로 보증.

## STRIDE (U3 초점)

| STRIDE | U3 대응 |
|--------|---------|
| Tampering | 상태 전이 서버 권위·조건부 UPDATE·불법전이 409 |
| Elevation of Privilege | 관리자 액션 ADMIN 게이트, 멘토 소유 경계 |
| Info Disclosure | 운영 허브 소유 경계 403, 비공개 정보 접근 제한 |
| Spoofing | 세션(U2) 전제 |
| Repudiation | 관리자 승인 액션 최소 기록 권고 [assumption] |

## 컴플라이언스

- 외부 규제 프레임워크 미적용(C2, U1 compliance). 모임 데이터=조직 내부.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 비ADMIN의 승인 액션 → 403. 타 멘토의 모임 편집 → 403. 이중 승인 경합 → 하나 성공/나머지 409. 종료 상태 재액션 → 409. ③ 세션 미종료 완료 시도 → 409.

## Assumptions & Open Questions

- **[assumption]** 관리자 액션 최소 감사 기록, 반려 사유 노출 범위.
- **[open]** 모임 상세의 비참여자 공개 범위(모집 중 공개 정보 수준).
- 운영 TLS·정식 감사는 범위 밖.
## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U3 Meeting, kind=service, XL — 상태머신 소유). 검토 범위 = U3 nfr 5종(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + U1 상속 계약(security-requirements.md·tech-stack-decisions.md). 결함 가정으로 반증 시도 후 blocking 미달성 → READY.

### Blocking (없음)

없음. 참조·순환·계약 정합·NFR 현실성을 결함 가정으로 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 U3 NFR을 구현 가능한 수준을 무너뜨리는 근거를 세우지 못함. 발견된 결함은 전부 provenance 주석의 인용 위생 nit이거나 하류(U4/U5/U8)로 정당하게 이월된 [open] 항목으로, 산출물 본문의 규범적 내용을 훼손하지 않음.

### Verification evidence (통과 항목)

- **RBAC — ADMIN 전이 게이트 정합.** security 인가절 "관리자 전이 액션(①/모집확정/②/③) role==ADMIN 아니면 403"은 business-logic-model W2 공통 전처리("Principal.role==ADMIN? 아니면 403")·BR-U3-1 전이표 트리거 역할(ADMIN)과 정합. U1 RBAC 계약(역할 게이트 위반 403, 4지점 승인=관리자 전용)을 정확히 승계. (인용 ID는 S1 참조 — 실질은 W2/BR-U3-1이 백킹.)
- **MENTOR 소유 경계.** "개설·문항 편집은 role=MENTOR + 소유 멘토"는 W1(createMeeting role==MENTOR else 403; upsertQuestions 소유 멘토 403)·BR-U3-3와 일치. 타인 모임 편집 403 명시.
- **상태 전이 동시성(조건부 UPDATE→409).** security STRIDE Tampering·scalability 동시성·reliability 데이터 무결성·TD-U3-1이 모두 `WHERE status=<expected>` 조건부 UPDATE(0 rows→409)로 일관 기술. BR-U3-1 "동시 전이 경합: 낙관적 락 또는 조건부 UPDATE 직렬화, 실패 시 409"와 정합. DB 원자성으로 애플리케이션 락 불요 — 단일 인스턴스(NFR4) 전제에서 타당, 과설계 없음.
- **③ 완료 U5-read 전제 + TOCTOU.** reliability가 "U5 read 검증 후 전이(ADR-007 R-2)·완료 액션 트랜잭션 내 status 재확인(TOCTOU 최소화)"으로 기술 — W2 completeMeeting(status==IN_PROGRESS else 409; allSessionsEnded not→409)·BR-U3-5와 정합. status 재확인은 조건부 UPDATE로 이중 완료를 차단. U5 read 실패 시 5xx/명시적 오류·silent 진행 금지(construction guardrail)로 장애 경로도 명시.
- **FE-composition 운영 허브(백엔드 U3→U4/U8 순환 없음).** security(운영 허브 자기 모임만·U4/U8 각 403 집행)·performance(FE 병렬 호출)·reliability(부분 실패 graceful degradation)·TD-U3-4(FE 병렬 조합, business-logic-model W4)가 일관. W4가 백엔드 listMyMeetings=U3 소유만 반환, U4/U8 read는 FE hubScreen 조합으로 분리 — functional-design 리뷰 B1(RESOLVED)과 회귀 없이 일치. U3 depends_on=[U1,U2] 유지, U3↔U4/U8 백엔드 순환 없음.
- **U1 상속·무모순.** CC-1 에러 매핑(400/401/403/404/409·ErrorPayload·한국어)은 business-rules 에러절이 "U1 CC-1 상속"으로 승계. 시크릿 비커밋·정적분석·의존성 스캔(team-practices)·에러 비노출·enumeration 방지가 U1 security와 정합. tech-stack 개요가 enum varchar+CHECK·OpenAPI·Flyway를 U1 TD-1/TD-2/TD-3 상속으로 명시 — 모순 없음.
- **NFR 현실성(로컬 파일럿).** perf 목록<1s·전이<500ms·상세<500ms·허브<2s는 NFR3(1~2초 가이드) 대비 보수적, 엄격 SLA 아님(가이드 명시). scalability 단일 JVM·단일 DB로 NFR2(수십) 충족·수평 확장 범위 밖(NFR4). reliability SLA/SLO 없음·HA·백업 범위 밖(NFR4)·상태머신 무결성/트랜잭션 원자성 집중(NFR5 영속). "단일 DB로 99.99%" 류의 과장·모순 없음. 과설계(워크플로우 엔진·이벤트 소싱)는 TD-U3-2/범위 밖에서 명시적으로 배제.
- **보안 깊이(devsecops).** 전이 변조 방지(서버 권위·조건부 UPDATE)·권한 상승(ADMIN 게이트)·정보 노출(허브 소유 경계 403)이 STRIDE 표로 커버. 검증 시나리오(비ADMIN 승인→403, 타 멘토 편집→403, 이중 승인→409, 종료 상태 재액션→409, ③ 세션 미종료→409)가 quality 테스트로 직접 검증 가능.
- **tech-stack TD-U3-1..4 방어성.** TD-U3-1(조건부 UPDATE/낙관적 락)=BR-U3-1 정합; TD-U3-2(명시적 전이표, 워크플로우 엔진 미도입)=6전이 소규모 상태머신에 타당, ADR-006 단일 소유 인용; TD-U3-3(survey_question 정규화, 응답은 U8 소유)=문항 빌더 정합; TD-U3-4(FE 병렬 조합)=W4 순환 회피 결정과 일치. 모두 근거·대안·범위 밖이 기록됨.
- **Epistemic 상태.** 감사 로깅([assumption], U1과 정합)·문항 보기 저장 형태(text[]/json [assumption])·조건부 UPDATE vs @Version([assumption])·아카이빙([open])·비참여자 공개 범위([open])·U4/U5 read 포트 시그니처([open])가 전부 태깅됨. 확정 규약으로의 silent promotion 없음.
- **센서.** required-sections: H2 개수 perf 4 / security 7 / scalability 5 / reliability 6 / tech-stack 4 — 전부 ≥2 (grep 확인). upstream-coverage: 5개 파일 헤더가 business-logic-model.md·business-rules.md·requirements.md를 모두 참조. linter/type-check: TS/JS/TSX 코드펜스 0건(grep 확인, TD-U3-1의 UPDATE는 인라인 SQL) → 대상 없음, N/A.

### Suggestions (non-blocking)

- **S1 — provenance 주석의 FR 인용 위생.** security-requirements 헤더 주석 `requirements.md(...FR2.2/6.1/7.3 관리자 승인)`에서 (a) "6.1"은 ②시작 승인의 US-6.1을 의도한 것으로 보이나 FR6.1(멘토 세션 날짜 지정, U5/U6 영역)로 읽혀 오인용 — functional-design 리뷰 S4가 정정한 "FR6.1 오인용" 회귀 우려; (b) "FR7.3"(멘토 성공 기준)은 관리자 ③ 완료의 근거로는 부정확 — 정본은 FR7.2(관리자 직접 ③, business-rules BR-U3-5가 FR7.2로 올바로 인용). 인가절 인용 `403(BR-U3-2)`도 실질 백킹은 W2/BR-U3-1 트리거 역할이며 BR-U3-2는 승인 지점 관계 규칙. 셋 다 주석/괄호 인용의 nit로 본문 규범 내용·구현 가능성에는 영향 없음 → non-blocking. `US-6.1`·`FR7.2`·(인가절) `W2/BR-U3-1`로 정정 권장.
- **S2 — 허브 지연 예산의 합성성 명시.** performance 표의 운영 허브 `<2초` 근거가 "U3+U4+U8 병렬 호출 **합산**"으로 병렬(max)과 합산(sum)이 문구상 상충. 본문은 "병렬 호출(직렬 대기 회피)"로 올바르나, 표 문구를 "병렬 → 최장 호출로 바운드"로 정정하고 이 예산이 U4/U8 각자의 지연 예산 충족에 의존함(U3 단독 보증 불가)을 한 줄 명시하면 합성 예산의 소유 경계가 분명해진다.
- **S3 — 허브 read의 소유 인가 정합(관찰).** security "신청자·설문 응답 열람은 소유 멘토/관리자만(U4/U8이 각 403 집행)"은 U4/U8이 모임 소유(meeting.mentorId, U3 데이터)를 알아야 집행 가능함을 함의한다. U8 depends_on에 U3가 있어 U8→U3(및 U4→U3) 역방향 read는 의존 방향과 일치하고 순환을 만들지 않으므로 아키텍처상 건전 — U3 산출물의 결함 아님. 다만 "U4/U8의 허브 read 인가가 모임 소유 기준으로 성립함"을 U4/U8 functional-design에서 계약 정합 항목으로 추적 권장(현재 U4/U5 read 포트 시그니처가 [open]으로 이월된 것과 함께).
- **S4 — U5-read → status write 잔여 TOCTOU(관찰).** reliability의 TOCTOU 재확인은 status 컬럼(조건부 UPDATE로 방어)에 한정되며, U5 세션 종료 상태가 read 시점과 commit 사이에 변할 수 있는 잔여 창은 다루지 않는다. 단일 인스턴스·관리자 액션 전제에서 창이 미미하고 이중 완료는 조건부 UPDATE로 차단되므로 non-blocking. U5 read 포트 확정 시 "완료 트랜잭션 내 U5 재조회 또는 스냅샷 정합" 여부를 U5 functional-design에서 마감 권장.

Verdict: READY
