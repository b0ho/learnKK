# Tech Stack Decisions — U6 Content (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U6 Content(service). 출처: business-logic-model.md(BLOB 업/다운로드), business-rules.md(BR-U6-2 형식/크기), requirements.md(C1·FR4.4 BLOB·A1 20MB). U1 tech-stack 상속. U6는 첨부 저장·검증 기술 선택. -->

## 개요

U1 스택·계약 도구 상속. U6는 첨부 BLOB 저장·형식 검증의 구체 기술을 확정.

## U6 기술 선택

### TD-U6-1. 첨부 저장 — PostgreSQL bytea (OQ4 결정)

- **결정:** `bytea` 컬럼(ADR-004). 20MB 상한(A1) 하에서 전량 로드 수용.
- **근거:** DB 트랜잭션 내 원자 저장(메타-데이터 정합), 파일시스템 경로 노출 없음(보안). C2(외부 클라우드 금지)로 S3 등 불가.
- **대안:** PostgreSQL Large Object(LO) — 진정한 스트리밍(청크 read) 지원, 진행 시 메모리 절감. 20MB 초과·동시 대용량 다운로드 빈번 시 전환 [assumption].
- **Reversibility:** 중간(저장 방식 마이그레이션 필요).

### TD-U6-2. 파일 검증 — 화이트리스트(결정) + 매직넘버(권고)

- **결정(OQ4):** contentType 화이트리스트(PDF/이미지 png·jpg·jpeg·gif·webp/오피스/txt). 확장자 신뢰 안 함. (OQ4 = 화이트리스트 + 크기 상한.)
- **권고 [assumption](OQ4 밖 보안 강화):** 매직넘버 확인(Apache Tika 등)으로 위조 확장자·실행 파일 차단. OQ4 범위가 아니므로 확정이 아닌 보안 권고로 둔다(security-requirements·business-rules와 상태 통일).

### TD-U6-3. 업로드/다운로드 — 멀티파트 + 스트림 응답

- **결정:** 업로드는 멀티파트, 다운로드는 응답 스트림(`Content-Disposition: attachment`). Spring `StreamingResponseBody`/`InputStreamResource` 활용.
- **근거:** 브라우저 인라인 실행 방지(XSS), 응답 단계 메모리 완화.

## 범위 밖

- 객체 스토리지(S3, C2 금지)·CDN·바이러스 스캔(후속). CI/CD·운영(C3).

## Assumptions & Open Questions

- **[decided/OQ4]** bytea·20MB·형식 화이트리스트. (매직넘버 검증은 OQ4 밖 보안 권고 [assumption].)
- **[open]** LO 전환(스트리밍 필요 시), 바이러스 스캔 도입(후속).
- **[open]** U4 `isParticipant` read 포트(인가) — U4 계약 추가 필요.
