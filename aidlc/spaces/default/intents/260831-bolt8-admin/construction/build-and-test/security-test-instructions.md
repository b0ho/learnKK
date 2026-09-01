# Security Test Instructions — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(devsecops 지원). Standard: 인가 경계 단위/슬라이스 검증. -->

## 초점
Bolt 8 보안 표면 = 전 모임 횡단 조회(운영 현황)의 접근 통제 — 관리자 전용 read.

## 검증 항목 (테스트로 커버)
- **인가(RBAC)**: `GET /api/admin/monitoring/meetings`는 ADMIN 전용 — MENTOR/MENTEE 403 `MONITORING_FORBIDDEN`(서비스 계층 가드), 미인증 401(SessionAuthInterceptor `/api/**` 보호).
- **입력 검증**: status 화이트리스트(enum 파싱 실패 400 VALIDATION_FAILED), sort 필드 화이트리스트(id/createdAt/title — INVALID_SORT_FIELD), size [1,100] 클램프(과대 페이지 요청 차단).
- **정보 노출 최소화**: 행에 멘토 닉네임만 노출(사번·이메일 등 식별 정보 제외). 쓰기 액션 없음 — 모니터링 경로로 상태 변경 불가(승인 액션은 기존 U3/U5 라우트의 자체 가드 유지).
- **에러 응답**: 전역 ErrorPayload(code/message) — 스택트레이스 비노출.

## 실행
- `cd backend && ./gradlew test --tests "com.learnkk.admin.*"` (401/403/400 케이스 포함).
