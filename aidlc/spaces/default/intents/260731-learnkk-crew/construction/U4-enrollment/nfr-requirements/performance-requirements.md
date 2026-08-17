# Performance Requirements — U4 Enrollment (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U4 Enrollment(service). 출처: business-logic-model.md(선착순 정원 apply·현황), business-rules.md(BR-U4-1 동시성), requirements.md(NFR2·NFR3). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·체감 1~2초(NFR3). U4는 **선착순 신청 동시성**이 성능 관심(정원 직렬화가 처리량에 영향).

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| 신청(apply) | < 1초 | 모임 단위 직렬화 구간 짧음(count+insert) |
| 취소 | < 500ms | 단건 상태 갱신 |
| 신청자 목록 | < 1초 | meeting_id 인덱스 + 페이지네이션 |
| 멘티 현황(화면 조합) | < 2초 | U4+U5+U3 병렬 → 최장 호출로 바운드 |

## 핵심 성능 고려

- **정원 직렬화 처리량:** apply는 모임 단위 어드바이저리 락(또는 SERIALIZABLE)으로 직렬화 — 같은 모임에 동시 신청이 몰리면 순차 처리. 파일럿 규모(정원 수십)라 락 구간이 짧아 병목 미미. 서로 다른 모임 신청은 병렬(모임 단위 락이라 상호 무간섭).
- **인덱스:** `enrollment(meeting_id, status)` 인덱스로 활성 count·신청자 목록 효율.
- **현황 화면:** FE 병렬 호출(U4/U5/U3), 최장 호출로 바운드.

## Assumptions & Open Questions

- **[assumption]** enrollment(meeting_id,status) 인덱스.
- **[open]** 극단 동시 신청(정원 대비 폭주) 시 락 경합 — 파일럿 규모 밖.
- 엄격 부하 테스트는 performance-validation(범위 밖).
