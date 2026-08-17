# Business Rules — U6 Content (learnKK / 런크크)

<!-- functional-design 산출물. Unit=U6 Content(service). 스토리 US-4.1a/4.1b/4.2/4.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U6·참여자 열람 U4 read), requirements.md(FR4.1~4.5·A1 20MB·OQ4), components.md(C5), component-methods.md(PostService/AttachmentService/NoticeService), services.md(BLOB 스트리밍), U1 business-rules(CC-1·인가). 첨부 BLOB(ADR-004). -->

## 개요

U6는 게시글·첨부·공지의 규칙을 소유한다. 작성은 소유 멘토, 열람은 참여자(멘토+APPLIED 멘티). 첨부 형식/크기 검증·BLOB 저장. U1 CC-1 상속.

## BR-U6-1. 게시글 작성 (US-4.1a)

- 작성자는 role=MENTOR + 대상 모임의 소유 멘토(U3 `meeting.mentorId` read)만. 아니면 403.
- 본문(body) 필수, 첨부 0개 허용(FR4.1) — 파일 없이 글만 가능. 본문 누락 400.
- 주차(week)는 모임 weeks 범위 내 [assumption] 검증(초과 400).

## BR-U6-2. 첨부 파일 검증 (US-4.1b, FR4.3/4.4, OQ4)

- **형식 화이트리스트:** PDF, 이미지(png/jpg/jpeg/gif/webp), 오피스(docx/xlsx/pptx), txt. 그 외 contentType → 400 `ATTACHMENT_TYPE_NOT_ALLOWED`.
  - 검증은 확장자만이 아니라 **실제 콘텐츠 타입(매직 넘버) 확인** 권고(위조 확장자 방지).
- **크기 상한:** 파일당 **20MB**(A1 확정). 초과 → 400 `ATTACHMENT_TOO_LARGE`(또는 413).
- **저장:** `bytea`(ADR-004) + 메타(fileName·contentType·sizeBytes·uploaderId).
- 첨부 개수 상한 [assumption]: 게시글당 예: 10개(구현 확정).

## BR-U6-3. 열람 권한 — 참여자만 (US-4.2)

- 게시글·첨부 다운로드는 **참여자만**: 대상 모임의 소유 멘토(U3 read) 또는 APPLIED 멘티(U4 read). 관리자도 열람 [assumption].
- 비참여자 → **403 `CONTENT_FORBIDDEN`**.
- 인가 판정: U6 백엔드가 U3(멘토)·U4(신청) Service read 조합. U6→U4 read는 비순환(U4는 U6 비의존).

## BR-U6-4. 공지 (US-4.3, FR4.5)

- 소유 멘토만 작성(403 경계). 열람은 참여자(BR-U6-3 동일).

## BR-U6-5. 인가 요약

- 작성(게시글/공지/첨부): 소유 멘토.
- 열람/다운로드: 참여자(멘토/APPLIED 멘티/관리자).
- 위반 403.

## 에러 처리 (U1 CC-1 상속)

- 형식/크기/본문 400, 인가 403, 미존재 404. ErrorPayload·한국어. 첨부 형식/크기 코드는 `<DOMAIN>_<REASON>`(U1 규약).

## Assumptions & Open Questions

- **[decided/OQ4]** 형식 화이트리스트(PDF/이미지/오피스/txt)·20MB(A1)·bytea(ADR-004).
- **[assumption]** 주차 범위 검증, 첨부 개수 상한(10), 관리자 열람 허용, 매직넘버 검증.
- **[open]** 게시글/공지 편집·삭제 정책. U3 멘토·U4 참여자 read 포트 시그니처(U3/U4 정합).
