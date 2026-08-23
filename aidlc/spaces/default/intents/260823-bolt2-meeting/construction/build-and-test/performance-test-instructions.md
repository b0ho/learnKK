# Performance Test Instructions — Bolt 2 Meeting 완성 (learnKK)

<!-- build-and-test 산출물(quality 리드). 출처: code-generation-plan.md·code-summary.md, U3 nfr-requirements/performance-requirements.md. 로컬 MVP 범위 — 파일럿 가이드만, 정식 부하는 이월. -->

## 범위

로컬 MVP·설계 전용 후속 구현 단계로, 정식 부하 테스트는 범위 밖. 다음 가이드는 파일럿 규모 확인용.

## 관찰 포인트 (Bolt 2)

- **상태 전이 쿼리**: T3~T6 조건부 UPDATE는 PK 기준 단일 행 갱신 — O(1). 경합 시에도 락 경합 최소(단일 행).
- **listMyMeetings**: `findByMentorId` — `idx_meetings_mentor_id` 인덱스 활용, 페이지네이션(U1 규약)으로 결과 제한.
- **listRecruiting**: `idx_meetings_status` 활용.

## 파일럿 확인 방법

- 소규모 시드 데이터(모임 수십~수백 건)에서 목록/전이 응답이 체감 지연 없이 반환되는지 수동 확인.
- 정식 부하(동시 사용자·처리량 목표·회귀 감지)는 CI/성능 파이프라인으로 이월.

## 이월

- 부하 도구(k6/Gatling)·처리량 목표·응답시간 SLO는 후속 워크플로우(operation/performance-validation).
