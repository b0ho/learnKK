<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->

- 2026-07-31T00:00:00Z — 사용자 지침: 사용자가 확인할 답변·질문·산출물 내용은 한글로 작성하고 고유어/기술 용어는 영어 그대로 사용한다. 워크플로우 전반에 적용되는 표준 지침이므로 게이트의 §13 learnings에서 project.md로 승격 검토 대상.

- 2026-07-31T00:05:00Z — 답변 로그(aidlc-log.ts answer)가 자가응답 방지 가드로 거부됨(self-guided 파일편집 모드에서 human-turn 마커 미인식 추정). 질문 파일이 답변의 원천이므로 진행에는 영향 없음. 다음 스테이지에서 guided 모드면 정상 기록될 것으로 예상.
- 2026-07-31T00:06:00Z — 수료 기준이 답변으로 확정됨: 멘티는 기간 내 출석율 80% 달성 시 수료, 멘토는 기간 내 정해진 학습 활동 회수 전부 진행 시 완료 인정. (초기 UA였던 '수료 임계값' 해소)
- 2026-07-31T00:06:30Z — 디자인 레퍼런스 방향: 온라인 클래스/코호트 러닝(클래스101·인프런 결)로 결정(Q5=B). rough-mockups/refined-mockups에서 이 방향 채택.