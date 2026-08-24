# Requirements Analysis — 확인 질문 (ux-bugfixes)

> 12개 버그픽스 중 구현 방향이 갈리는 5개 지점만 확인합니다. 나머지 7개(설문 쉼표 입력, 승인 확인 다이얼로그, 자료실 강조, 피드백/사전설문 분리, 관리자 네비 '관리')는 이견 없는 개선이라 그대로 확정합니다. `[Answer]:`에 A~E 또는 X로 답해 주세요.

## Q1. 관리자 승인 "되돌리기(revert)" 범위 (#5)
어떤 전이까지 되돌릴 수 있어야 하나요?
- A. 전진 승인만 역전이: RECRUITING→PENDING, READY_TO_START→RECRUITING, IN_PROGRESS→READY_TO_START, COMPLETED→IN_PROGRESS
- B. A + 반려/모집취소도 복구: REJECTED→PENDING, CANCELLED→RECRUITING
- C. 직전 1단계만 (현재 상태 기준 한 단계 뒤로) — B 포함 (권장)
- D. 되돌리기 없음
- X. Other (please specify)

[Answer]:a

## Q2. 멘토 "세션 완료 처리"의 의미 (#8)
세션을 완료 처리하면 무엇이 달라져야 하나요?
- A. 완료 표시만(뱃지) — 출석/수료 판정엔 영향 없음
- B. 완료 처리한 세션은 시간과 무관하게 "종료된 세션"으로 간주 → 모임 완료(③ T6) 게이트를 통과시킴 (권장)
- C. B + 완료 처리해야만 출석율의 분모(전체 세션 수)에 포함
- X. Other (please specify)

[Answer]:b, 단 정해진 세션 시간이 지나면 자동 완료 처리

## Q3. 삭제한 세션의 출석 데이터 (#7)
세션 삭제 시 이미 있는 출석 기록은?
- A. 함께 삭제(CASCADE) — 출석율 자동 재계산 (권장; DB FK가 이미 ON DELETE CASCADE)
- B. 출석 기록이 있으면 삭제 금지
- X. Other (please specify)

[Answer]:a

## Q4. 사전설문 문항 수정 허용 시점 (#10)
문항 추가/수정을 언제까지 허용하나요?
- A. 모집확정(READY_TO_START)까지만
- B. 진행 중(IN_PROGRESS)까지 허용, 완료/취소면 잠금 (권장)
- C. 완료(COMPLETED) 후에도 항상 허용
- X. Other (please specify)

주의: 문항을 바꾸면 기존 응답과 매칭이 깨질 수 있습니다. 이때 처리:
- (i) 기존 응답 유지(문항 삭제돼도 응답 레코드는 남김) (권장)
- (ii) 문항 교체 시 해당 모임 응답 초기화
[Answer]:a

## Q5. 러닝 취소 후 재신청 방식 (#12)
- A. 취소했던 신청 레코드를 재활성화(APPLIED로 되돌림, 신청일 갱신) — 모임이 RECRUITING이고 정원 여유 있을 때 (권장)
- B. 새 신청 레코드 생성(취소 이력 여러 건 남김) — UNIQUE 제약 완화 필요
- X. Other (please specify)

[Answer]:a

## Consolidated Summary Confirmation

수집된 답변 요약:
- **Q1 되돌리기(#5) = A**: 전진 승인만 역전이(직전 1단계). RECRUITING→PENDING_APPROVAL, READY_TO_START→RECRUITING, IN_PROGRESS→READY_TO_START, COMPLETED→IN_PROGRESS. 반려(REJECTED)·모집취소(CANCELLED)는 되돌리기 대상 아님.
- **Q2 세션 완료(#8) = B(+자동)**: 완료 처리한 세션은 시간과 무관하게 "종료"로 간주해 모임 완료(③ T6) 게이트를 통과시킴. 추가로, 세션 예정 시간(시간창)이 지나면 자동으로 종료 간주(현행 시간 파생 유지). 즉 세션 종료 = 수동 완료 OR 시간창 경과.
- **Q3 세션 삭제(#7) = A**: 삭제 시 출석 기록도 함께 삭제(CASCADE), 출석율 자동 재계산.
- **Q4 사전설문 수정(#10) = A**: 모집확정(READY_TO_START)까지 수정 허용, 진행 중(IN_PROGRESS)부터 잠금. (백엔드 lock은 현행 유지 — IN_PROGRESS부터 잠금이 이미 그러함. 프론트에 기존 모임의 문항 편집 진입점만 추가.) 응답 시작은 IN_PROGRESS 이후라 문항-응답 불일치는 발생하지 않음.
- **Q5 재신청(#12) = A**: 취소했던 신청 레코드를 재활성화(APPLIED 복귀, 신청일 갱신). 모임 RECRUITING·정원 여유 시.
- 나머지 7건(쉼표 입력·승인 확인 다이얼로그·자료실 강조·피드백/사전설문 분리·네비 '관리')은 이견 없이 확정.

Does this all look correct before I generate the requirements artifact?
- Looks correct
- Request changes

[Answer]: Looks correct

