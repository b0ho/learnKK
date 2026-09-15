# Security Test Instructions — apply-button-state (devsecops 관점)

신규 공격면 없음. 점검 결과:
- 모집 목록(`GET /api/meetings?status=recruiting`)은 기존과 동일하게 **공개(비인증)** 유지. 신규 필드 `enrolledCount`/`full`은 집계 수치·불리언으로 민감정보 아님(개별 신청자 신원 미노출).
- 내 신청 조회(`GET /api/enrollments/mine`)는 기존 인증 경로 그대로 — 호출자 본인 데이터만 반환. FE는 로그인 MENTEE에서만 호출.
- 인원 집계 쿼리는 파라미터 바인딩(JPQL `:ids`, `:status`) — 인젝션 없음.
- 권한/정원 강제(BR-U4-1 원자적 체크, 409 방어)는 변경 없음 — 표시 로직은 서버 방어를 대체하지 않고 보완만 함.
```
cd backend && ./gradlew test --tests "com.learnkk.enrollment.service.EnrollmentServiceTest"
```
