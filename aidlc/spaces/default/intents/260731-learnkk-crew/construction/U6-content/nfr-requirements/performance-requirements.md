# Performance Requirements — U6 Content (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U6 Content(service). 출처: business-logic-model.md(업로드·다운로드·목록), business-rules.md(BR-U6-2 20MB), requirements.md(NFR2·NFR3·A1 20MB). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U6는 **첨부 BLOB 업/다운로드**가 성능·메모리 관심(bytea 전량 로드).

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| 게시글 목록 | < 1초 | meeting_id 인덱스, 첨부는 메타만(BLOB 제외) |
| 게시글 작성 | < 500ms | 단건 insert |
| 첨부 업로드(≤20MB) | < 3초 | 크기 의존, bytea write |
| 첨부 다운로드(≤20MB) | 크기·대역 의존 | bytea read + 응답 스트림 |

## 핵심 성능·메모리 고려

- **BLOB 메모리(bytea):** bytea는 DB read 시 전량 메모리 로드 → 20MB × 동시 다운로드 수만큼 힙 사용. 파일럿 규모(동시 수 명)에선 수용, 동시 대용량 다운로드 제한 [open]으로 완화.
- **목록 조회 시 BLOB 배제:** 게시글 목록은 첨부 **메타데이터만** 조회(data bytea 제외) — 목록에서 BLOB 로드 금지(N+메가바이트 로드 방지).
- **다운로드:** data는 다운로드 시점에만 로드, 응답 스트림으로 전달(Content-Disposition).

## Assumptions & Open Questions

- **[assumption]** 목록은 BLOB 제외 메타만 로드, meeting_id 인덱스.
- **[open]** 동시 대용량 다운로드 제한(메모리 보호), LO 전환(진정 스트리밍 필요 시).
- 엄격 부하 테스트는 performance-validation(범위 밖).
