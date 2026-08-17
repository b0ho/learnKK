# Scalability Requirements — U6 Content (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U6 Content(service). 출처: business-logic-model.md(BLOB), business-rules.md(20MB), requirements.md(NFR2·NFR4·FR4.4 BLOB). 파일럿·단일 인스턴스. 첨부 저장 증가가 관심. -->

## 개요

requirements NFR2(모임 수십)·NFR4(단일 인스턴스). 수평 확장 범위 밖. 첨부 BLOB이 DB 크기 증가 주 요인.

## 부하·저장 전망

- 첨부 BLOB: 게시글×첨부×20MB → DB 크기 증가 주 요인. 파일럿(모임 수십·주차별 자료)은 수 GB 규모 예상 — 단일 PostgreSQL 수용.
- 게시글/공지 텍스트: 완만.

## 확장 전략

- 단일 DB로 목표 충족. 첨부 증가가 커지면(범위 밖) 객체 스토리지(S3 등) 분리 검토 — 단 C2(외부 클라우드 금지)라 로컬 파일시스템/LO가 대안. 현재 bytea 유지.
- BLOB이 백업·복제 비용을 키우나 파일럿엔 무관(백업 범위 밖).

## 동시성

- 업로드/다운로드 동시성은 메모리(bytea 전량 로드)가 제약 — 동시 대용량 다운로드 제한 [open]으로 관리.

## Assumptions & Open Questions

- **[assumption]** 첨부 저장은 bytea 단일 DB, 파일럿 수 GB 규모.
- **[open]** 첨부 대량화 시 저장 분리(LO/로컬 FS, C2 준수), 오래된 자료 아카이빙 — 범위 밖.
