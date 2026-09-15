# Performance Test Instructions — apply-button-state

부하테스트 대상 없음(UI 표시 버그픽스). 성능 관련 확인 포인트:
- 모집 목록 인원 집계는 **단일 배치 쿼리**(`countByMeetingIdInAndStatusGrouped`)로 수행 — 모임 수 N에 대한 N+1 없음.
- 검증: 목록 조회 시 발행 쿼리가 (목록 1 + 카운트 1)임을 로그/`spring.jpa.show-sql`로 확인 가능.
