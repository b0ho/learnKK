# 보안 테스트 지침 — learnKK ux-bugfixes (devsecops 관점)

## 적용 범위 (Minimal)
전용 SAST/DAST는 이번 스코프 밖. 다만 이번 변경이 인가 경계를 넓히므로 아래 권한 검증을 단위/웹 테스트로 커버한다.

## 인가 회귀 체크리스트
- 관리자 전용: `POST /api/admin/meetings/{id}/revert`, `GET /api/admin/meetings` → 비관리자 403(`requireAdmin`). MeetingApprovalServiceTest.revert_nonAdmin_forbidden403 커버.
- 소유 멘토 전용: `DELETE /api/sessions/{id}`, `POST /api/sessions/{id}/complete` → 타 멘토/멘티 403(`requireOwningMentor`). SessionServiceTest.deleteSession_nonOwner_forbidden403 커버.
- 문항 편집: 프론트 `/meetings/:id/questions-edit`는 RequireRole(MENTOR)로 보호, 백엔드는 소유 멘토 + IN_PROGRESS lock 유지.
- 재신청(FR-12): 멘티만(`isMentee`), 재활성화는 정원 검사(advisory lock) 하에서만.

## 비밀/입력
- 신규 코드에 하드코딩 시크릿 없음. 입력은 기존 검증 계약(전역 `{code,message,details}`) 재사용. 세션 완료 플래그는 boolean로 주입 경로 없음(멘토 액션으로만 set).
