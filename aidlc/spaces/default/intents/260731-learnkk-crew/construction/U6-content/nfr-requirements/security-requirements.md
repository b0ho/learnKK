# Security Requirements — U6 Content (learnKK / 런크크)

<!-- nfr-requirements 산출물(architect 리드 + devsecops·compliance·quality). Unit=U6 Content(service). 출처: business-logic-model.md(업로드·열람·인가), business-rules.md(BR-U6-2 형식/크기·BR-U6-3 참여자), requirements.md(NFR6·NFR8·FR4.3/4.4). U1 보안 계약 상속. 첨부 파일 보안이 핵심. -->

## 개요

U6는 첨부 파일 업/다운로드가 있어 **파일 보안**이 가장 중대한 관심. 형식/크기 검증·참여자 인가·안전한 다운로드. U1 cross-cutting 상속.

## 인가

- **작성(게시글/첨부/공지):** 소유 멘토만(403, BR-U6-1/5). 타 모임 작성 불가.
- **열람/다운로드:** 참여자(멘토/APPLIED 멘티/관리자)만 → 비참여자 403 `CONTENT_FORBIDDEN`(BR-U6-3). 인가는 매 다운로드 요청마다 서버 검증(직접 URL 접근 방어).

## 파일 업로드 보안 (핵심, devsecops)

- **형식 화이트리스트:** PDF/이미지/오피스/txt만(BR-U6-2). 확장자뿐 아니라 **매직넘버(실제 콘텐츠 타입)** 검증 권고 — 위조 확장자·실행 파일 차단. 위반 400.
- **크기 상한:** 20MB(A1). 초과 400/413 — DoS(대용량 업로드) 완화.
- **저장:** bytea(ADR-004) — 파일시스템 경로 노출 없음(경로 탐색 공격 표면 축소). 원본 fileName은 메타로만 저장, 다운로드 시 안전하게 세팅(Content-Disposition, 파일명 새니타이즈).
- **악성 파일:** 파일럿은 바이러스 스캔 미포함 [assumption](후속). 서버는 첨부를 실행하지 않음(다운로드 전용). **잔여 위험:** 오피스 문서(docx/xlsx/pptx)의 매크로는 열람자(클라이언트)에서 실행될 수 있어 서버 non-execution·Content-Disposition으로 방어되지 않음 — 조직 내부 자료 전제의 수용 위험(바이러스 스캔 도입 시 완화).
- **다운로드 응답:** `Content-Type`은 저장된 contentType, `Content-Disposition: attachment`로 브라우저 인라인 실행(특히 HTML/SVG) 방지 — XSS 완화.

## 데이터 보호

- 첨부는 참여자 전용 콘텐츠 — 인가 없는 접근 403. 입력 검증 400.
- SVG 등 스크립트 내장 가능 형식은 화이트리스트에서 제외(이미지는 png/jpg/jpeg/gif/webp만) — 저장 XSS 방지.

## STRIDE (U6 초점)

| STRIDE | U6 대응 |
|--------|---------|
| Tampering | 형식/크기 검증, 소유 멘토 작성 |
| Info Disclosure | 참여자 인가 403, 직접 URL 방어 |
| Elevation of Privilege | 작성=멘토, 열람=참여자 게이트 |
| DoS | 20MB 상한, 동시 다운로드 제한 [open] |
| Spoofing | 매직넘버 검증(위조 형식) |

## 컴플라이언스

- 외부 규제 미적용(C2). 첨부=조직 내부 학습자료.
- 시크릿 비커밋·정적분석·의존성 스캔(team-practices) 상속.

## 검증 시나리오 (quality)

- 비참여자 다운로드 → 403. 허용 외 형식(exe/svg) → 400. 20MB 초과 → 400/413. 위조 확장자(exe→pdf 리네임) → 매직넘버로 400. 직접 attachmentId URL 접근(비참여) → 403. 다운로드 Content-Disposition=attachment 확인.

## Assumptions & Open Questions

- **[assumption]** 매직넘버 검증, 바이러스 스캔 미포함(후속), 관리자 열람 허용.
- **[open]** 동시 대용량 다운로드 제한, 바이러스 스캔 도입(후속).
- SVG 등 스크립트 내장 형식 제외 — 이미지는 래스터만.
## Review

**Reviewer:** aidlc-architecture-reviewer-agent
Review type: 적대적 아키텍처 검토 (nfr-requirements, Unit U6 Content, kind=service). 검토 범위 = U6 nfr 5종(security·performance·scalability·reliability·tech-stack) + consumed(business-logic-model·business-rules·requirements) + U1 상속(security·tech-stack). 파일 보안·bytea 저장/메모리 tradeoff를 반증 대상으로 삼아 hunting 후 blocking 미달성 → READY.

반증 시도 세 갈래: (1) 파일 업로드 보안 통제가 실행/XSS 벡터를 남기거나 functional-design 규칙과 어긋나는가, (2) bytea 결정이 스트리밍을 과대약속하거나 ADR-004/A1/FR4.4와 모순인가, (3) 인식적 태깅이 [assumption]/[open]을 조용히 확정으로 승격했는가. 어느 것도 blocking으로 세우지 못했다. 다만 (3)에서 매직넘버의 상태가 아티팩트 간에 서로 모순된다는 실증 근거를 확보했다 — 구현 가능성을 무너뜨리진 않아 non-blocking으로 판정하되 top suggestion으로 명시한다.

### Blocking (없음)

없음. 개발자가 아키텍트 추가 질의 없이 U6 파일 업/다운로드·인가·bytea 저장을 구현할 수 있는 수준을 무너뜨리는 근거를 세우지 못함.

### 검증 근거 (Verification evidence)

- **형식 화이트리스트 정합 — PASS.** security-requirements(PDF/이미지/오피스/txt, 이미지 래스터만, SVG 제외)는 business-rules BR-U6-2(png/jpg/jpeg/gif/webp·docx/xlsx/pptx·txt) 및 functional-design W2(400 `ATTACHMENT_TYPE_NOT_ALLOWED`)의 충실한 구체화. requirements FR4.3 "문서 위주(PDF/이미지/오피스 등)"와 일치. SVG 스크립트 내장 형식 제외 → 저장 XSS 벡터 차단, 화이트리스트가 SVG를 애초에 포함하지 않으므로 이중 방어로 정합.
- **크기 상한 — PASS.** 20MB(A1 "기본 제안 20MB … functional-design 최종 확정")·초과 400/413은 BR-U6-2·functional-design W2(`ATTACHMENT_TOO_LARGE`)와 일치. 업로드 DoS 완화 논리 성립.
- **경로 탐색·인라인 실행 방어 — PASS.** bytea 저장으로 파일시스템 경로 미노출(path-traversal 표면 축소)은 ADR-004·FR4.4(PostgreSQL BLOB 직접 저장)와 정합. 다운로드 `Content-Disposition: attachment` + fileName 새니타이즈는 브라우저 인라인 실행(HTML/SVG)·XSS 완화로 논리 성립, functional-design W3(Content-Type·Content-Disposition)과 일치.
- **매 요청 참여자 인가(직접 URL 방어) — PASS.** "인가는 매 다운로드 요청마다 서버 검증"은 functional-design W3 `download(requesterId, attachmentId)` → `assertParticipant(requesterId, attachment.post.meetingId)`와 정확히 대응. BR-U6-3(참여자만·비참여자 403 `CONTENT_FORBIDDEN`)과 일치. 클라이언트 신뢰 없이 서버 권위 판정 → U1 RBAC 계약(경계 위반 403) 승계.
- **bytea 메모리/성능 정직성 — PASS.** performance-requirements가 "bytea는 DB read 시 전량 메모리 로드 → 20MB × 동시 다운로드 수만큼 힙"을 명시하고, 목록은 BLOB 배제(메타만)·다운로드 시점 로드·응답 스트림 전달로 구분. functional-design [note]("bytea는 응답 스트림 수준만 스트리밍 — DB read는 전량 로드")와 정합. 진정한 스트리밍이 아님을 은폐하지 않고 LO 대안(TD-U6-1)·동시 대용량 다운로드 제한 [open]으로 완화 — ADR-004/A1/FR4.4와 모순 없음.
- **NFR 현실성(로컬 파일럿) — PASS.** 성능은 가이드(NFR3 1~2초, SLA 아님), 확장은 단일 인스턴스(NFR2 수십·NFR4)·수평 확장 범위 밖·S3는 C2로 배제(로컬 FS/LO 대안 명시), 신뢰성은 업로드 단일 트랜잭션 원자성(NFR5)·SLA 없음(NFR4). CDN/객체스토리지/AV는 후속으로 정확히 이연 — 과설계 없음.
- **U1 상속 무모순 — PASS.** U1 security는 "첨부 BLOB(U6)"·"첨부 형식 화이트리스트·크기 상한은 U6 소관"으로 위임, U1 tech-stack은 "첨부 BLOB(ADR-004)"로 저장 타입을 이연. U6가 ADR-004를 bytea로 해소(TD-U6-1)하는 것은 위임받은 결정이며 U1과 충돌 없음. CC-1 에러 매핑·`<DOMAIN>_<REASON>` 코드·ErrorPayload 비노출·RBAC 403을 승계. at-rest 암호화·백업 범위 밖(NFR4)도 U1과 일치.
- **STRIDE(U6 초점) — PASS.** Tampering(형식/크기 검증), Info Disclosure(참여자 403·직접 URL 방어), EoP(작성=멘토·열람=참여자 게이트), DoS(20MB·동시 제한 [open]), Spoofing(매직넘버) 모두 상위 규칙과 대응. U1 STRIDE-lite와 계층적으로 정합(U1=cross-cutting, U6=도메인 세부).
- **센서 — PASS.** required-sections: security 8 H2, performance 4, scalability 5, reliability 6, tech-stack 4 (모두 ≥2). upstream-coverage: 5개 파일 모두 헤더 prose에 business-logic-model·business-rules·requirements를 참조(+본문에서 BR-U6-2/3·NFR2~6/8·FR4.3/4.4·A1 인용). 코드펜스에 TS/JS/TSX 언어 태그 없음(Spring 클래스명은 산문 언급) → linter/type-check 대상 없음.

### Suggestions (non-blocking)

- **S1 (top) — 매직넘버 검증의 인식적 상태가 아티팩트 간 모순.** security-requirements("매직넘버 … 검증 권고", Assumptions `[assumption]`), business-rules BR-U6-2("권고", `[assumption]`), business-logic-model(`[assumption]`)은 매직넘버를 **권고/가정**으로 둔다. 반면 tech-stack-decisions TD-U6-2는 "**결정:** contentType 화이트리스트 + 매직넘버 확인(Apache Tika 등)"으로 단정하고 Assumptions에 "**[decided/OQ4]** … 화이트리스트+매직넘버"로 묶는다. 그러나 requirements OQ4는 "형식 화이트리스트와 최종 크기 상한"만을 범위로 하며 매직넘버 검증을 포함하지 않는다 — 즉 매직넘버를 `[decided/OQ4]`에 귀속한 것은 근거 초과다. 동일 산출물 세트 안에서 한 통제가 3곳 `[assumption]` / 1곳 `[decided]`로 갈리면 개발자는 "파일럿에서 매직넘버를 필수 구현하는가"를 판단하려 아키텍트에게 물어야 할 수 있다. **Blocking으로 올리지 않은 이유:** 어느 아티팩트도 매직넘버를 배제하지 않고 도구(Tika)까지 지목하므로 안전한 기본값(구현)으로 모호성이 해소되어 구현 가능성은 유지된다. 조치: tech-stack에서 매직넘버를 `[assumption]`(권고)로 통일하거나, 나머지 3곳을 `[decided]`로 승격하되 OQ4가 아닌 별도 보안 결정으로 귀속하라(OQ4=화이트리스트+크기만).
- **S2 — 오피스 문서의 클라이언트측 매크로 잔여 위험이 "서버는 실행하지 않음" 논리로 완전히 덮이지 않음.** "이미지/문서 위주라 실행 방지(서버는 첨부를 실행하지 않음, 다운로드 전용)"은 서버측 실행만 다룬다. docx/xlsx/pptx는 매크로를 담아 열람자(클라이언트)에서 실행될 수 있어 서버 non-execution·Content-Disposition:attachment로는 방어되지 않는다. 바이러스 스캔 이연([open])이 남기는 잔여 위험 항목으로 이 클라이언트측 매크로 경로를 한 줄 명시하면 위협 모델의 정직성이 완결된다(파일럿·조직 내부 자료 전제라 수용 가능한 잔여 위험이나 명시 권고).
- **S3 — 이미지 확장자 목록 드리프트.** 데이터 보호 절은 이미지를 `png/jpg/gif/webp`로 적어 `jpeg`를 누락(BR-U6-2는 `png/jpg/jpeg/gif/webp`). jpg/jpeg는 MIME `image/jpeg`를 공유하고 검증이 contentType 기준이라 기능상 무해하나, business-rules와 문자열을 일치시켜 화이트리스트를 단일 표기로 정렬하라. (functional-design 검토 S3와 동일 계열 드리프트.)
- **S4 — isParticipant read 포트의 미해소 계약 의존.** 참여자 인가(assertParticipant)는 U4 `isParticipant` read 포트에 의존하나 tech-stack-decisions는 이를 `[open]`("U4 계약 추가 필요")로 둔다. functional-design 검토(S1)가 이미 EnrollmentService에 해당 메서드가 없고 `listMyEnrollments`로 자기-참여 확인이 가능함을 기록했다. nfr 단계가 이를 `[open]`으로 정직하게 유지한 것은 적절하며 비순환(U4는 U6 비의존)이라 non-blocking이나, U4 계약 정합 시 포트 시그니처를 확정해 이 인가 경로의 미해소 의존을 닫아야 한다.

Verdict: READY
