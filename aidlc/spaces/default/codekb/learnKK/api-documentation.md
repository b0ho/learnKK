# API 문서 — learnKK (`contracts/openapi.yaml` 0.7.0-bolt7)

## 인증 / 사용자
- `POST /api/auth/signup` — 가입(nickname·password(8+)·employeeNo·role[MENTOR|MENTEE]). ADMIN 거부.
- `POST /api/auth/login` — 로그인 → {token, role}. `POST /api/auth/logout`.
- `GET/PUT /api/users/me/profile` — 프로필 조회/수정.

## 모임 (U3)
- `POST /api/meetings` (멘토), `GET /api/meetings/{id}`, `GET /api/meetings?status=recruiting`(공개), `GET /api/meetings/mine`(멘토 본인 전체).
- `GET/PUT /api/meetings/{id}/questions` — 사전설문 문항(멘토 소유, IN_PROGRESS+ 이후 lock).

## 관리자 승인 (U3, ADMIN)
- `POST /api/admin/meetings/{id}/approve` (T1), `/reject` (T2), `/confirm-recruitment` (T3/T4, proceed+reason), `/approve-start` (T5), `/complete` (T6, 세션 종료 게이트).
- `POST /api/admin/meetings/{id}/completions/{menteeId}/approve` (④ 수료 확정).

## 신청 (U4)
- `POST /api/meetings/{id}/enrollments` (멘티 신청), `DELETE /api/meetings/{id}/enrollments/mine` (취소), `GET /api/meetings/{id}/applicants` (멘토/관리자), `GET /api/enrollments/mine`.

## 세션/출석/수료 (U5)
- `POST /api/meetings/{id}/sessions` (생성, 멘토·IN_PROGRESS), `PUT /api/sessions/{id}` (일정 변경), `GET /api/meetings/{id}/sessions`.
- `POST /api/sessions/{id}/attendance` (멘티 체크인), `GET /api/meetings/{id}/my-attendance`.
- `POST /api/meetings/{id}/completions/compute`, `GET /api/meetings/{id}/completions`.
- **없음(이번 버그픽스로 추가 대상)**: 세션 DELETE, 세션 완료 처리, 관리자 상태별 모임 목록, 모임 역전이(revert).

## 콘텐츠/쪽지/설문 (U6/U7/U8)
- 콘텐츠: `/api/meetings/{id}/posts`·`/notices`, `/api/posts/{postId}/attachments`, `/api/attachments/{attachmentId}`.
- 쪽지: `/api/messages`·`/threads`·`/threads/{id}`·`/unread-count`·`/recipients`.
- 설문: `/api/meetings/{id}/survey-answers`(POST, ②후), `/survey-answers/mine`, `/mentees/{menteeId}/survey-answers`(멘토/관리자), `/api/meetings/{id}/feedback`(POST/GET).

## 공통 규약
- 응답 DTO는 camelCase. 에러는 `{code, message, details}` + HTTP 상태(400/401/403/404/409).
- 계약 테스트 `OpenApiContractTest`가 DTO↔스키마 일치를 23케이스로 검증(현재 green).
