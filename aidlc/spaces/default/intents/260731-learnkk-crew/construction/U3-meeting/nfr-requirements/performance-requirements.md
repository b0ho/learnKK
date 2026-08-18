# Performance Requirements — U3 Meeting (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U3 Meeting(service). 출처: business-logic-model.md(상태전이·목록·운영허브), business-rules.md(BR-U3-1 전이·조건부 UPDATE), requirements.md(NFR2 규모·NFR3 성능 1~2초). U1 baseline 상속. -->

## 개요

파일럿 규모(NFR2)·응답 체감 1~2초(NFR3). U3는 모임 목록 조회(멘티 다수 접근)와 상태 전이(관리자·경합)가 성능 관심.

## 응답 시간 목표 (가이드)

| 작업 | 목표 | 근거 |
|------|------|------|
| 모집 목록 조회(listRecruiting) | < 1초 | status 인덱스 + 페이지네이션 |
| 모임 상세 | < 500ms | 단건 + 사전설문 템플릿 |
| 상태 전이 액션 | < 500ms | 조건부 UPDATE 단건 |
| 운영 허브(화면 조합) | < 2초 | U3/U4/U8 병렬 호출 → 최장 호출로 바운드(U4/U8 각 지연 예산에 의존, U3 단독 보증 아님) |

## 핵심 성능 고려

- **목록 조회:** `meeting.status`에 인덱스 → RECRUITING 필터 효율. 파일럿 규모(수십 모임)라 부담 낮음. 페이지네이션(U1 규약).
- **상태 전이 조건부 UPDATE:** `WHERE status=<expected>` 단일 행 갱신 — 저비용. 경합 시 재시도 아닌 409 반환(BR-U3-1).
- **운영 허브:** FE가 U3/U4/U8 엔드포인트를 **병렬** 호출(직렬 대기 회피). 각 호출은 소유 Unit 성능에 의존.

## Assumptions & Open Questions

- **[assumption]** status 인덱스·페이지네이션 기본.
- **[open]** 목록 필터·정렬 옵션 확장 시 인덱스 재검토.
- 엄격 성능 검증은 performance-validation(범위 밖).
