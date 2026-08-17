# Performance Requirements — U8 Survey/Feedback (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U8 Survey/Feedback(service). 출처: business-logic-model.md(응답·피드백 제출/열람), business-rules.md, requirements.md(NFR2·NFR3). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U8은 응답·피드백 제출/조회 위주의 경량 CRUD로 성능 부담 낮음.

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| 사전설문 응답 제출 | < 500ms | 문항 read + upsert |
| 응답/피드백 조회 | < 1초 | meeting_id·mentee_id 인덱스 |
| 피드백 제출 | < 500ms | 단건 insert |

## 핵심 성능 고려

- 응답 제출은 U3 문항 read + 다건 upsert(문항 수만큼) — 파일럿 문항 수(수~십수 개)라 경량.
- 조회는 `survey_answer(meeting_id, mentee_id)`·`feedback(meeting_id)` 인덱스로 효율.

## Assumptions & Open Questions

- **[assumption]** survey_answer/feedback 인덱스.
- 엄격 부하 테스트는 performance-validation(범위 밖).
