# Security Requirements — U8 Survey/Feedback (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U8 Survey/Feedback(service). 출처: business-logic-model.md(응답·피드백·열람), business-rules.md(BR-U8-1~5 인가·게이팅), requirements.md(NFR6·NFR8·FR3.6/8.2). U1 보안 계약 상속. 응답·피드백 프라이버시가 핵심. -->

## 개요

U8은 학습자 응답·피드백(민감할 수 있는 의견)을 다루므로 **열람 권한 경계**가 핵심 보안 관심. U1 cross-cutting 상속.

## 인가 (핵심)

- **사전설문 응답 제출:** 참여 멘티 본인(②후, BR-U8-1). 비참여자 403.
- **사전설문 응답 열람(getAnswers, menteeId 인자):** 소유 멘토·관리자·본인(status 게이팅 없음). 타 모임 멘토·비인가 403.
- **피드백 제출:** 참여 멘티 본인.
- **피드백 열람(listFeedback, meetingId 단위):** 소유 멘토·관리자만 → **타 모임 멘토 403**(FR8.2, 멘티 열람 경로 없음).

## 데이터 보호 (프라이버시)

- 피드백은 소유 멘토·관리자 범위로만 노출. 사전설문 응답은 소유 멘토·관리자 + **본인(getAnswers는 menteeId 인자로 본인 열람 표현 가능)**. 멘티 상호 응답은 비공개. 입력 검증 400.
- 저장 XSS: 응답/피드백 텍스트 이스케이프 렌더(FE).
- 사전설문 응답은 ②시작 후에만 제출 가능(BR-U8-1) — 시점 무결성.

## STRIDE (U8 초점)

| STRIDE | U8 대응 |
|--------|---------|
| Info Disclosure | 열람 소유·역할 경계 403(타 모임 멘토·멘티 상호 비공개) |
| Elevation of Privilege | 제출=참여 멘티·열람=멘토/관리자 게이트 |
| Tampering | ②후 게이팅, 텍스트 이스케이프, 서버 권위 |
| Spoofing | menteeId=Principal(본인 제출) |

## 컴플라이언스

- 외부 규제 미적용(C2). 응답·피드백=조직 내부 학습 데이터.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 타 모임 멘토 피드백/응답 열람 → 403. 비참여자 응답 제출 → 403. ② 전 응답 제출 → 409. 멘티의 listFeedback 접근 → 403. 응답 XSS 페이로드 → 이스케이프.

## Assumptions & Open Questions

- **[decided]** 응답 열람 status 게이팅 없음(인가만), 피드백 열람 멘토/관리자만.
- **[assumption]** 텍스트 이스케이프, 필수 미응답 400.
- 운영 TLS·정식 감사는 범위 밖.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U8 Survey/Feedback, kind=service). 검토 범위 = U8 nfr 5종(performance·security·scalability·reliability·tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + U1 상속 계약(security-requirements.md). 반증 시도 후 blocking 미달성 → READY.

### Blocking (없음)

없음. 결함 가정(참조 깨짐·순환 의존·인가 경계 모순·NFR 과설계)으로 요구 ID·U8 functional-design·U1 계약을 교차 대조했으나, 개발자가 아키텍트 추가 질의 없이 이 NFR 세트를 구현 가능한 수준을 무너뜨리는 근거를 세우지 못함. 특히 직전 승인 수정(응답 읽기 IN_PROGRESS 미게이팅, 피드백 읽기 멘토/관리자 전용)의 회귀가 없음을 확인.

### 검증 근거 (Verification evidence)

- **인가 경계 — functional-design 정합(핵심).** security 인가 절 4항이 W1–W4/BR-U8-1~5와 일치:
  - 사전설문 제출 = 참여 멘티 본인(②후), 비참여자 403 — W1/BR-U8-1 일치.
  - `getAnswers`(menteeId 인자) 열람 = 소유 멘토·관리자·본인, **status 게이팅 없음** — W2/BR-U8-5 일치. 직전 승인 수정(읽기를 IN_PROGRESS로 게이트하지 않음)이 정확히 반영됨. COMPLETED 후에도 멘토·관리자 열람 가능(FR3.6·U3 허브·U9 모니터링) 유지.
  - 피드백 제출 = 참여 멘티 본인.
  - `listFeedback`(meetingId 단위, menteeId 없음) 열람 = **소유 멘토·관리자만, 타 모임 멘토 403, 멘티 열람 경로 없음** — W4/BR-U8-4/FR8.2 일치. getAnswers는 menteeId 보유(본인 열람 표현 가능)·listFeedback는 미보유(본인 열람 불가)의 시그니처 비대칭이 두 문서에서 동일하게 반영됨.
- **프라이버시** — 멘티 상호 응답 비공개·저장 XSS 이스케이프(FE)·②후 제출 시점 무결성이 STRIDE(Info Disclosure/Tampering) 표와 정합. 조직 내부 학습 데이터로 외부 규제 미적용(C2) — U1 컴플라이언스 절과 모순 없음.
- **NFR 현실성(로컬 파일럿)** — 제출 <500ms·조회 <1초·피드백 <500ms는 NFR3(1~2초 가이드) 하한이며 경량 CRUD에 타당(과설계 아님). scalability: 단일 JVM·단일 DB, 수평 확장 범위 밖(NFR4)·동시 수십(NFR2). reliability: SLA/SLO 없음(NFR4), 단일 트랜잭션 upsert·`unique(question,mentee)`·원자 insert·영속 보존(NFR5). 모두 파일럿 제약과 일치.
- **U1 상속** — 위반 403·입력 검증 400·②전 409 `PRESURVEY_NOT_OPEN`·404가 U1 CC-1 에러 매핑(business-rules 에러 처리)·RBAC 403 계약을 승계. 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속 명시. U1과 모순 없음.
- **기술 선택 정합** — TD-U8-1 `survey_answer(meetingId,questionId,menteeId,answerText)`+`unique(question,mentee)`, TD-U8-2 `feedback(meetingId,menteeId,content)`, TD-U8-3 ②후 게이팅=요청 시점 status read(조회 미게이팅)는 functional-design W1/W3/W4 및 성능 절 조회 인덱스 가정과 정합. getAnswers 조회 인덱스 survey_answer(meeting_id,mentee_id)는 [assumption]로 정직하게 태깅.
- **순환 의존성 — 없음.** U8은 U1/U3/U4를 read-in(정방향), read-out은 FE 조합(U3 허브)·U9 모니터링뿐. U3/U4가 U8에 역의존하지 않음. NFR 산출물은 이 방향성을 새로 위반하지 않음.
- **인식적 상태(Epistemic)** — `[decided]`(읽기 status 미게이팅·피드백 읽기 멘토/관리자 전용), `[assumption]`(텍스트 이스케이프·필수 미응답 400·feedback content 형태·인덱스·upsert), `[open]`(과정 설문 문항 구조·U3/U4 read 포트 시그니처·문항 삭제 참조 무결성·아카이빙) 모두 정직. 확정 규약으로의 암묵 승격 없음.
- **센서** — required-sections: H2 수 performance 4·security 7·scalability 4·reliability 6·tech-stack 4 (전부 ≥2) OK. upstream-coverage: 5개 파일 모두 business-logic-model·business-rules·requirements 참조 OK. TS/JS/TSX 펜스 코드 없음 → linter/type-check inert.

### Suggestions (non-blocking)

- **S1** — 데이터 보호 절 "응답·피드백은 소유 멘토·관리자 범위로만 노출"은 `getAnswers`의 **본인(self)** 열람 경로를 누락한다(인가 절·W2·BR-U8-5는 본인 포함). "멘티 상호 비공개"라는 취지는 옳으나, 응답에 한해 "본인 열람 허용" 한 줄을 프라이버시 절에도 병기하면 인가 절과의 표현 정합이 완결된다. (인가 절이 권위·정합하므로 구현 결함 아님 → 이월 불요.)
- **S2** — reliability의 "U3 BR-U3-7(②후 문항 편집 금지)" 및 tech-stack TD-U8-3의 "TD-U5류 스케줄러리스" 인용은 pass-list 밖(다른 Unit) 아티팩트를 참조한다. 두 건 모두 `[open]`/유추 성격이라 U8 구현이 이에 경성 의존하지 않으나, U3/U5 계약 확정 시 해당 규칙/결정이 source-of-truth임을 재확인하도록 open 항목에 남겨두길 권고.
- **S3** — reliability graceful degradation "피드백 열람 일부 실패 시 부분 표시"는 `listFeedback`가 `Feedback[]` 단건 조회임을 감안하면 "부분 실패" 단위가 모호하다. 다건 조합 화면(FE) 수준의 부분 표시인지, 서비스 반환 수준인지 code-generation 전에 한 줄 명확화 권고.

Verdict: READY
