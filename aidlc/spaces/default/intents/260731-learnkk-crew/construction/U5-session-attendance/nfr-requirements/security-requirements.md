# Security Requirements — U5 Session/Attendance (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U5 Session/Attendance(service). 출처: business-logic-model.md(출석·수료·④), business-rules.md(BR-U5-1~6 인가), requirements.md(NFR6·NFR8·FR7.1/7.2). U1 보안 계약 상속. 출석·수료 무결성이 핵심(성공지표). -->

## 개요

U5는 출석·수료 데이터의 무결성이 성공지표(80% 수료)와 직결되어 **위변조 방지·인가**가 핵심. U1 cross-cutting 상속.

## 인가

- **세션 관리:** 소유 멘토(U3 read)만 → 403.
- **출석 체크:** 참여 멘티 **본인**만(menteeId==Principal). 타인 대리 출석 불가(403). 참여자 아님 403.
- **수료 판정 조회:** 멘토(자기 모임)·관리자.
- **④ 수료 확정:** 관리자만(403 게이트). 자동 판정(computeCompletion)은 시스템, 확정은 관리자 권위.

## 데이터 무결성 (성공지표 직결)

- **출석 위변조 방지:** checkIn은 시간창 내 본인만(대리·소급 불가). `unique(session,mentee)` 멱등 — 중복 조작 무해. 서버 시간(now) 기준 판정 — 클라이언트 시간 신뢰 안 함.
- **수료 판정 무결성:** a*100≥80*S 정수 판정으로 부동소수 조작 여지 제거. 확정 시 스냅샷(a/S) 보존 — 사후 세션 변경이 확정 이력 훼손 못함. **stale 후보 방지(S4):** compute(후보 판정)~④ 확정 사이 세션 추가로 S가 바뀌면 실제 출석율이 80% 미만이 될 수 있으므로, ④ 확정은 **직전 재판정(현재 S 기준)을 선행**해 stale COMPLETION_CANDIDATE 확정을 막는다(A6 [open]과 연계).
- 출석 시각(checkedInAt)·확정 시각(approvedAt) 기록(비부인).

## STRIDE (U5 초점)

| STRIDE | U5 대응 |
|--------|---------|
| Tampering | 서버 시간 판정, 본인 출석, 정수 수료 판정, 스냅샷 |
| Spoofing | 대리 출석 불가(menteeId==Principal) |
| Elevation of Privilege | ④ 확정 ADMIN 게이트, 세션 관리 멘토 게이트 |
| Repudiation | checkedInAt·approvedAt 기록 |
| Info Disclosure | 수료/출석 조회 소유·역할 경계 403 |

## 컴플라이언스

- 외부 규제 미적용(C2). 출석·수료=조직 내부 학습 기록.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 대리 출석(타 멘티) → 403. 시간창 밖 출석 → 409. 미충족 멘티 ④ 확정 → 409. 비ADMIN ④ → 403. 클라이언트 시간 조작 출석 → 서버 now로 무효.

## Assumptions & Open Questions

- **[assumption]** 서버 시간 기준 시간창, 확정 스냅샷 보존.
- **[open]** 세션 소급 변경 제약(A6)이 확정 이력에 미치는 영향 — 스냅샷으로 격리.
- 운영 TLS·정식 감사는 범위 밖.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U5 Session/Attendance, kind=service, L). 검토 범위 = U5 nfr 5종(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + U1 상속(security-requirements.md). 반증 시도: ①출석·수료 무결성(80% 성공지표 백본), ②스케줄러리스 시간 판정, ③U1 계약 정합, ④파일럿 NFR 현실성. 세 축 모두 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정으로 요구 ID·U5 functional-design·U1 계약을 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 이 NFR 세트를 구현하는 것을 무너뜨리는 근거를 세우지 못함.

### Verification evidence (검증 근거)

- **출석 무결성 — PASS.** security 데이터 무결성/STRIDE가 (a) 서버 시간(now) 기준 창 판정(클라이언트 시간 불신), (b) `unique(session,mentee)` 멱등 upsert(중복 조작 무해), (c) 본인 self check-in만(menteeId==Principal, 대리 불가 403), (d) 참여자 아님 403을 모두 명시. BLM W2(step1 참여자·step2 IN_PROGRESS·step3 `now ∈ [scheduledAt, scheduledAt+checkInWindowMinutes]`·step4 멱등 upsert)·BR-U5-2·BR-U5-6과 축자 정합. 창 밖 → **409** `ATTENDANCE_WINDOW_CLOSED`로 U1 CC-1(타이밍/상태 충돌=409)과 일치 — BR-U5-2가 "400/409"로 흔들린 지점을 NFR 문서는 409로 정합화(개선).
- **수료 무결성 — PASS.** `a*100≥80*S` 정수 판정으로 부동소수 조작 여지 제거(BLM W3·BR-U5-4·unit-of-work 노트와 일치). ④ 확정 시 스냅샷(attendedCount/totalScheduled=a/S) 보존 → 사후 세션 변경이 확정 이력 훼손 못함(BR-U5-3·W4 "스냅샷 a/S 유지"). ④ ADMIN 게이트(비ADMIN 403), 미충족 확정 409는 W4·BR-U5-5·U1 BR-U1-5 승계.
- **스케줄러리스(ADR-005) — PASS, 신뢰성 단순화 근거 성립.** reliability "배치 잡 실패·중복 실행 리스크 없음", performance "백그라운드 잡 없음→유휴 부하 없음, 판정 요청 시점만", tech-stack TD-U5-1 "FE 클라이언트 타이머 트리거·서버는 검증만"이 BLM W2 노트·BR-U5-2와 일관. 서버 now 기반 판정이라 배치 이중실행/누락 표면 자체가 제거됨 — 신뢰성 축소 주장이 방어됨.
- **U1 상속 — 모순 없음.** RBAC 위반 403·상태 충돌 409·에러 비노출·CC-1은 U1 security-requirements의 규약을 정확히 승계. computeCompletion=시스템/확정=관리자 권위 분리는 U1 "4지점 승인=관리자 전용"과 정합.
- **파일럿 NFR 현실성 — PASS.** 출석 스파이크는 경량 시간창 비교+멱등 upsert로 단일 인스턴스 수용(NFR2 동시 수십·NFR4 단일 인스턴스). 수료 집계는 참여자×세션 온디맨드(캐시 없음)이며 computeCompletion <2s는 NFR3(1~2s 가이드) 내. 단일 인스턴스=단일 시간 소스로 클럭 정합 확보(reliability). SLA/HA/백업·스케줄러/배치 인프라·집계 캐시를 범위 밖으로 정확히 배제 — 과설계 없음.
- **인식적 상태 — PASS, 암묵 승격 없음.** [decided]: 스케줄러리스·정수 판정·멱등 upsert·mentee_completion U5 소유(tech-stack). [assumption]/[open]: 시간창 길이(120분)·온디맨드 집계(캐시 없음)·단일 시간 소스·세션 소급 변경(A6)·U3/U4 read 포트 시그니처 — 5종 파일에 걸쳐 일관 태깅. mentee_completion은 U5 소유 테이블·CompletionStatus는 U1 enum으로 소유 경계 정합.
- **센서 — PASS.** required-sections: 5종 파일 H2 각 4/7/4/6/4개(≥2). upstream-coverage: 5종 모두 헤더 소스주석에 business-logic-model.md·business-rules.md·requirements.md 참조.

### Suggestions (non-blocking)

- **S1 — computeCompletion(mentee_completion upsert 경로)의 인가 주체가 security 인가 절에 명시되지 않음.** 인가 절은 "수료 판정 조회(멘토·관리자)"와 "④ 확정(관리자)"만 게이트하고 computeCompletion은 "시스템"으로만 서술. computeCompletion은 mentee_completion에 쓰기(upsert)를 수행하므로, 노출 엔드포인트라면 트리거 권한을 명시하는 편이 완결적. **다만 재계산은 attendance/session에서 결정적·멱등 재산출이라 결과 위조가 불가**(비인가 트리거가 발생해도 올바른 값 재기록) → blast radius 최소, 무결성 위반 아님. 그래서 blocking이 아닌 위생 권고.
- **S2 — TD-U5-2 본문은 온디맨드 집계(캐시 없음)를 "결정"으로 서술하나 Assumptions 절과 performance/scalability는 [assumption]으로 태깅.** 에피스테믹 절이 [assumption]으로 올바르게 표기하므로 silent promotion 아님. 본문 프레이밍만 [assumption]과 정렬하면 문서 간 톤 일관.
- **S3 — TD-U5-3 `ON CONFLICT DO NOTHING`과 BLM W2 step5 `return AttendanceResponse` 간 구현 힌트 필요.** DO NOTHING은 충돌 시 행을 반환하지 않으므로, 멱등 재요청에서 AttendanceResponse를 돌려주려면 후속 SELECT(기존 행 조회)가 필요. 설계 결함 아님(멱등성·응답 모두 충족 가능) — 구현자 안내용 한 줄 권고.
- **S4 — compute→approve 사이 세션 변동 시 stale CANDIDATE 승인 여지.** 스냅샷은 **확정 후** 세션 변경으로부터 이력을 격리하나(security 주장 정확), computeCompletion(후보 판정) 이후·④ 확정 이전에 멘토가 세션을 추가하면(S↑) 실제 출석율이 80% 미만이 되어도 관리자가 재판정 없이 stale COMPLETION_CANDIDATE를 확정할 수 있음. BR-U5-3 "판정 시점 S"·edge case "세션 추가→재판정 반영"이 재판정을 전제하나, W4 approve는 status만 검사. 이미 [open] A6(세션 변경 통지·확정 이력 영향)으로 정직하게 플래그되어 있고 security 문서도 이를 [open]으로 격리 — 그래서 이월 불요·비blocking. security/reliability 문서에 "세션 변동 시 ④ 전 재판정 선행"을 한 줄 명시하면 성공지표 무결성 논증이 더 견고.

Verdict: READY
