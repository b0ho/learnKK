# Security Test Instructions — ux-bugfixes-2

범위 밖(bugfix, minimal). 신규 보안 테스트를 추가하지 않으며, 기존 권한 경계를 회귀 검증한다.

- FR-6 완료 처리: 여전히 ADMIN 전용(requireAdmin, 403 유지). 세션 게이트만 제거되고 인가는 불변.
- FR-7 멘토 수료 판정: `compute`는 owning-mentor OR admin(백엔드 재검증), ④ 확정은 ADMIN 전용 유지(FE에서 멘토에게 확정 버튼 미노출).
- FR-2 my-learning 스코프 라우트: feedback-view는 RequireRole[MENTOR,ADMIN], questions-edit는 RequireRole[MENTOR]로 기존과 동일 가드. 서버도 소유권 재검증.
- FR-11 시드: 개발용 계정/데이터(공통 비밀번호 password123). 운영 배포 전 반드시 정리·비밀번호 변경.
