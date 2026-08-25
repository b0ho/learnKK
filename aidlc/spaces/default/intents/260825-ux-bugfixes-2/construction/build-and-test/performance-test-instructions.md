# Performance Test Instructions — ux-bugfixes-2

범위 밖(bugfix, minimal). 별도 성능 테스트를 추가하지 않는다.

- 변경은 기존 read 경로에 소규모 조합(출석 세션 id 목록, 문항 텍스트 결합, 승인 큐 카운트 조회)만 추가하며 신규 N+1 위험은 제한적이다(모두 인덱스된 단일 모임 스코프 read).
- 로컬 MVP 운영 규모 가정상 페이지네이션/부하 테스트는 이월.
