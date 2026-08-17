# Domain Entities — U6 Content (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U6 Content(kind=service). 스토리: US-4.1a/4.1b/4.2/4.3(unit-of-work-story-map.md). 출처: unit-of-work.md(U6=C5 게시글 본문+첨부 BLOB·공지·참여자 열람 U4 read), requirements.md(FR4.1~4.5·A1 20MB·OQ4 형식/상한), components.md(C5·소유 데이터 post/post_attachment/notice), component-methods.md(PostService/AttachmentService/NoticeService), services.md(첨부 BLOB 스트리밍), U1(ErrorPayload·Pagination). 첨부 BLOB(ADR-004). 참여자 열람은 U4/U3 read. Entity API 비노출(NFR8). -->

## 개요

U6는 C5(자료·공지) 도메인 엔티티를 소유한다: `post`(주차 게시글), `post_attachment`(첨부 BLOB+메타), `notice`(공지). 열람 권한(참여자)은 U4(신청)·U3(멘토) read로 판정.

## 엔티티

### Post (주차 게시글)

US-4.1a. 멘토가 주차별 게시글 작성(본문 + 첨부 0..n).

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | U3 모임 |
| `authorId` | BIGINT (FK→user) | NOT NULL | 멘토 |
| `week` | int | NOT NULL | 주차(FR4.1) |
| `body` | text | NOT NULL | 본문(첨부 없어도 글만 가능, FR4.1) |
| `createdAt`/`updatedAt` | timestamptz | | 작성시각(FR4.2) |

- 본문 필수, 첨부는 선택(0개 이상, FR4.1). 첨부 없이 글만 허용.

### PostAttachment (첨부 파일 BLOB)

US-4.1b, FR4.3/4.4.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `postId` | BIGINT (FK→post) | NOT NULL | 소속 게시글 |
| `fileName` | varchar | NOT NULL | 원본 파일명 |
| `contentType` | varchar | NOT NULL | MIME(화이트리스트 검증) |
| `sizeBytes` | bigint | NOT NULL | 상한 검증 |
| `data` | bytea | NOT NULL | BLOB(ADR-004) |
| `uploaderId` | BIGINT (FK→user) | | 업로더(FR4.4) |
| `createdAt` | timestamptz | | |

- **저장 방식(OQ4 결정):** PostgreSQL `bytea`(ADR-004). 20MB 상한(A1) 하에서 전량 로드가 수용 가능(파일럿). 진정한 스트리밍 필요 시 Large Object(LO)로 전환 [assumption] — 현재 bytea.
- **메타데이터:** fileName·contentType·sizeBytes·uploaderId·postId(주차는 post.week)로 FR4.4 충족.

### Notice (공지)

US-4.3, FR4.5.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | |
| `authorId` | BIGINT (FK→user) | NOT NULL | 멘토 |
| `body` | text | NOT NULL | 공지 내용 |
| `createdAt` | timestamptz | | |

## 관계·통합 지점 (읽기 교차참조)

- `meetingId` → meeting(U3). 멘토 소유(작성 권한)는 U3 `meeting.mentorId` read.
- **참여자 열람 권한(US-4.2):** 게시글·첨부 열람은 **참여자만**(멘토 또는 APPLIED 멘티). 판정은 U6 백엔드가 (a) U3 멘토 여부 read + (b) **U4 신청 여부 read**로 수행. 비참여자 403.
  - **의존 방향:** U6 → U4 read edge를 추가한다(참여자 인가). **U4는 U6에 의존하지 않으므로 비순환**(U4 depends_on=U1,U2,U3). units-generation 요약표의 U6 depends_on=[U1,U2,U3]에 **U4를 read-only로 확장**하는 것으로, unit-of-work.md U6 노트("참여자 열람 권한은 U4 read")가 예고한 read다. U4 `EnrollmentService` 인터페이스 경유(테이블 직접 접근 아님, 모듈 소유 준수).

## 생명주기

- Post/Notice: 작성(활성) → (편집/삭제 정책 [open]). 첨부는 Post에 종속(Post 삭제 시 첨부 제거).

## Assumptions & Open Questions

- **[decided/OQ4]** 첨부 저장=bytea(ADR-004), 상한=20MB(A1 확정), 형식 화이트리스트=PDF·이미지(png/jpg/jpeg/gif/webp)·오피스(docx/xlsx/pptx)·txt(business-rules BR-U6-2와 동일). LO 전환은 스트리밍 필요 시.
- **[assumption]** 게시글/공지 편집·삭제 정책, 첨부 개수 상한.
- **[open]** U4 참여자 read 포트(`isParticipant`)·U3 멘토 read 시그니처는 U4/U3 계약 정합.
- 참여자 인가 read edge U6→U4는 비순환(U4는 U6 비의존).
