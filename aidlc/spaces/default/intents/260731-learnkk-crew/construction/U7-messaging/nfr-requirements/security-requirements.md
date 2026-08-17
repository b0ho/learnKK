# Security Requirements — U7 Messaging (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U7 Messaging(service). 출처: business-logic-model.md(canMessage·스레드), business-rules.md(BR-U7-1 권한·BR-U7-3 스레드), requirements.md(NFR6·NFR8·FR5.1). U1 보안 계약 상속. 쪽지 프라이버시가 핵심. -->

## 개요

U7은 사적 통신을 다루므로 **쪽지 프라이버시·권한 경계**가 핵심 보안 관심. U1 cross-cutting 상속.

## 인가 (핵심)

- **발신 권한 경계:** canMessage(BR-U7-1) — 멘토=활성 등록 관계 멘티/관리자, 멘티=자기 모임 멘토/관리자, 관리자=전원. 위반 403 `MESSAGE_FORBIDDEN`. 서버 권위(FE 대상 목록은 UX 보조).
- **스레드 열람:** 본인 참여 스레드만 → 타인 스레드 403. 스레드 ID 추측 접근 방어(참여자 검증).

## 데이터 보호 (프라이버시)

- 쪽지 본문은 참여자 2인만 열람. 관리자라도 임의 사용자 간 사적 스레드를 열람하지 않음(관리자는 자신이 참여한 스레드만) [assumption] — 관리자 감독 열람 권한은 요구에 없으므로 부여하지 않음.
- 입력 검증 400. 저장 XSS: 본문은 텍스트로 저장·이스케이프 렌더(FE), HTML 미해석.

## STRIDE (U7 초점)

| STRIDE | U7 대응 |
|--------|---------|
| Info Disclosure | 스레드 참여자 경계 403, 본문 프라이버시 |
| Elevation of Privilege | canMessage 관계 게이트(무관계 발신 차단) |
| Spoofing | senderId=Principal(사칭 불가) |
| Tampering | 본문 이스케이프, 서버 권위 |
| Repudiation | createdAt·senderId 기록(발신 이력) |

## 컴플라이언스

- 외부 규제 미적용(C2). 쪽지=조직 내부 사적 통신 — 최소 노출.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 무관계 멘토→멘티 발신 → 403. 타인 스레드 열람 → 403. 사칭 발신(senderId 위조) → Principal로 차단. 본문 XSS 페이로드 → 이스케이프 렌더.

## Assumptions & Open Questions

- **[assumption]** 관리자 감독 열람 미부여(참여 스레드만), 본문 이스케이프.
- **[open]** 관계 판정 read 포트(U3/U4 정합).
- 운영 TLS·정식 감사는 범위 밖.

## Review

**Reviewer:** aidlc-architecture-reviewer-agent — re-review iteration 2 (적대적 아키텍처 검토, nfr-requirements, Unit U7 Messaging, kind=service). 검토 범위 = U7 nfr 5산출물(performance/security/scalability/reliability/tech-stack) + consumed(business-logic-model.md·business-rules.md·requirements.md) + U1 상속 security. 이번 라운드 초점: iteration 1의 blocking B1 수정 검증 + 회귀 확인.

iteration 1은 폴링 미확인 집계 인덱스가 소유 functional-design 메시지 스키마에 없는 `recipient` 컬럼을 전제한다는 B1으로 NOT-READY였다. 수정(option a)이 적용되어 phantom 컬럼이 제거되고, 인덱스·부하 주장 모두 실제 스레드-조인 모델에 정합화되었음을 확인했다. 회귀 없음. B1을 다시 세우려 했으나 근거가 사라졌다.

### B1 재검증 — RESOLVED

- **phantom `recipient` 컬럼 제거 — 확인.** performance-requirements "핵심 성능 고려"가 이제 "미확인 집계는 functional-design 모델대로 **본인 참여 스레드(message_thread participantA/B) 조인 + message(readAt IS NULL AND senderId != user)** 로 산출(별도 recipient 비정규화 컬럼 없음)"로 서술. 인덱스도 `message(recipient, readAt)` → **`message(thread_id, sender_id, read_at)` + `message_thread(participantA/participantB)`** 로 재표기. 세 컬럼(thread_id, sender_id, read_at)은 모두 business-logic-model W1 insert 컬럼 `(threadId, senderId, body, createdAt)` + `readAt`에 실재하며, message_thread(participantA/participantB)는 TD-U7-2 정규화 pair 스레드와 정합. 존재하지 않는 스키마 요소 없음.
- **폴링 부하 "여유" 주장의 근거 정합 — 확인.** scalability 부하 전망이 "스레드 조인 + read_at 인덱스 집계로 여유(recipient 파생, 비정규화 컬럼 없음)"로 갱신되어 performance와 동일한 실제 집계 형태에 기댄다. 두 산출물이 서로, 그리고 W3(`sum(readAt IS NULL AND senderId != userId) 전 스레드`)·BR-U7-2(수신자 = 스레드 참여 파생)와 일관. iteration 1이 지적한 "존재하지 않는 스키마에 기댄 현실성 주장" 소멸.
- **정합 [note] 추가 — 확인.** performance Assumptions에 "[note] 미확인 집계는 스레드 참여 파생(비정규화 recipient 컬럼 미도입) — functional-design W3/BR-U7-2와 정합"이 명시되어, 하류 개발자가 recipient 컬럼 도입 여부를 아키텍트에게 되물을 필요가 없다. NOT-READY 기준(아키텍트 추가 질의 없이 구현 가능?) 해소.

### 회귀 확인 (regression check) — 통과

- **신규 모순 없음.** 인덱스 재표기가 tech-stack TD-U7-2(message_thread participantA/participantB 정규화), TD-U7-3(U3/U4 Service read, 비순환), business-logic-model 통합 지점(read-out 없음)과 충돌하지 않음. 발신 목표 `< 500ms`·unreadCount `< 200ms` 응답 목표도 새 집계 형태와 무모순.
- **iteration 1 통과 항목 유지 — 재확인.** canMessage↔BR-U7-1(활성 등록 관계·403 MESSAGE_FORBIDDEN), 스레드 참여자 경계(BR-U7-3/5·W2), 관리자 감독 열람 미부여 [assumption](FR5.1 근거·최소권한), 저장 XSS 이스케이프, 폴링 부하 산술(수십×30~60초≈초당 수 회), 과잉설계 배제(WebSocket/SSE/MQ 범위 밖), U1 상속(CC-1·RBAC 403·ErrorPayload·senderId=Principal), 비순환(U7→U3/U4 forward-only), epistemic 태깅 — 모두 iteration 1에서 PASS였고 수정으로 훼손되지 않음.
- **S1 반영 — 확인.** reliability "데이터 무결성"이 "readAt 갱신은 멱등 — 조건부 갱신(`UPDATE ... WHERE read_at IS NULL`, first-writer-wins)으로 동시 getThread write 경합에도 안전"으로 갱신. iteration 1 S1(동시 getThread write 경합) 해소.
- **S3 반영 — 확인.** scalability [open]이 "메시지 아카이빙(=조회 계층 분리, 파기 아님 — NFR5 영속 보존 유지)"으로 못박혀 아카이빙 vs NFR5 영속 보존의 하류 혼선 제거.
- **센서 — PASS.** required-sections H2: security 8·performance 4·scalability 4·reliability 6·tech-stack 4(모두 ≥2). upstream-coverage: business-logic-model(canMessage·스레드·unreadCount·폴링), business-rules(BR-U7-1/2/3/4), requirements(NFR2/3/4/5/6/8·FR5.1/5.2/5.3)가 5산출물 prose에 참조됨 → 충족.

### 잔여 non-blocking

- **S2(이월, 미차단)** — performance 발신 `< 500ms`는 canMessage의 U3+U4 두 Service read를 포함한다. read 포트 시그니처가 [open]인 현재, "관계 판정 read 왕복 수(U3 1회 + U4 1회 가정)"를 한 줄 명시하면 목표 검증 가능성이 올라간다. 관계 read 포트 [open] 항목과 함께 하류에서 자연 해소되므로 차단하지 않음.

Verdict: READY
