<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
- 2026-08-25T11:30:00Z — FR-1 "활성 탭 재클릭 새로고침": 현재 라우트와 동일한 하단 탭을 다시 눌렀을 때 해당 탭의 루트로 이동하며 데이터를 재조회(reload)하는 것으로 해석. react-router NavLink는 같은 경로 재클릭 시 no-op이므로 명시적 리셋 필요.
- 2026-08-25T11:30:00Z — FR-7 "멘토에 대한 수료 판정 버튼": 소유 멘토가 자기 모임의 수료 판정(compute)을 버튼으로 직접 실행할 수 있어야 한다는 의미로 해석(백엔드는 이미 owning-mentor OR admin 허용). 완료 처리 이후 운영 허브의 수료 판정 영역에 compute 버튼 노출.
- 2026-08-25T11:30:00Z — FR-6 "완료 처리는 세션 미완료여도 가능": ③ 완료(T6) 게이트(allScheduledSessionsEnded)를 제거/완화하여 세션 종료 여부와 무관하게 관리자가 완료 처리 가능하도록 변경.
- 2026-08-25T11:45:00Z — FR-2 재해석(사용자 반려 후 정정): "자료실·피드백을 내 러닝에서"는 진입점 추가가 아니라 **탭 컨텍스트 유지** 요구. 현재 `/meetings/{id}/content|feedback` 진입 시 하단 탭이 '모임'으로 전환되는 버그를 고쳐, 내 러닝에서 진입하면 '내 러닝' 탭 하이라이트가 유지되어야 함. 구현: `/my-learning/...` 스코프 경로로도 동일 페이지 진입 가능하게 하여 NavLink 활성 유지.

## Deviations
- 2026-08-25T11:30:00Z — 인셉션 게이트(reverse-engineering)는 기존 CodeKB(2026-08-24)를 재생성 없이 채택(사용자 승인 "1"). 버그픽스 범위에 충분.

## Tradeoffs
- 2026-08-25T11:30:00Z — 풍성한 시드(FR-11)는 고정 ID 대신 자연키(employee_no/제목) 기반 SELECT 삽입 + ON CONFLICT DO NOTHING으로 멱등·기존 데이터 비파괴. 기존 수동 데이터와 공존.

## Open questions
<!-- none — 사용자가 11개 수정 항목을 명시적으로 지정함 -->
