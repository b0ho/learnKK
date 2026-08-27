# Requirements Analysis — 확인 질문 (apply-button-state bugfix)

대상: "모집중 모임" 목록(`MeetingListPage`)의 신청 버튼 상태 관련 2건.

- **Bug 1** 최초 로드 시 이미 신청한 모임도 "신청" 버튼이 활성 → 클릭해야 "이미 신청한 모임입니다" 노출.
- **Bug 2** 정원이 찬 모임도 "신청" 활성 → 클릭 시에만 "모집 정원이 마감되었습니다" 방어.

각 질문의 `[Answer]:` 태그에 A~E 또는 X(직접 입력)로 답해 주세요.

---

## Q1. Bug 2(정원 마감) 수정 범위

현재 목록 응답(`MeetingSummary`)에는 `capacity`만 있고 **현재 신청 인원 수가 없어** 프론트가 로드 시점에 마감 여부를 알 수 없습니다. 마감 배지/비활성을 로드 시 표시하려면 백엔드가 인원 수를 노출해야 합니다.

- A. 백엔드까지 완전 수정 — `MeetingSummary`에 신청 인원 수(`enrolledCount`) + 파생 마감 여부(`full`) 추가, `listRecruiting`에서 모임별 APPLIED 인원 집계, OpenAPI 계약·백엔드 테스트 갱신, FE는 "마감" 배지 + 버튼 비활성. (정확·완전, 변경 범위 큼)
- B. 프론트 전용 — Bug 1(신청 상태 반영)만 이번에 수정. Bug 2는 기존 409 방어 유지(정원 찬 모임도 클릭 전까진 버튼 활성). (범위 작음)
- C. 백엔드는 `enrolledCount`만 추가하고 마감 판정(`enrolledCount >= capacity`)은 프론트에서 계산 (`full` 필드 없이)
- X. Other (please specify)

[Answer]:a

## Q2. 로드 시 이미 신청한 모임(Bug 1)의 버튼 표시

- A. "신청완료" + 비활성 — 신청 직후 상태와 동일한 문구
- B. "신청됨" + 비활성 — MyLearningPage에서 쓰는 "신청됨" 문구와 통일
- C. "신청완료" 배지로 표시하고 버튼은 숨김
- X. Other (please specify)

[Answer]:a

## Q3. (Q1에서 A 또는 C 선택 시) 정원이 찬 모임이고 내가 신청하지 않은 경우 표시

- A. "마감" 배지 + "마감" 라벨의 비활성 버튼
- B. "마감" 배지만 표시, 신청 버튼은 숨김
- C. 상태 배지(모집중) 옆에 별도 "마감" 배지, 버튼은 비활성 유지("신청")
- X. Other (please specify)

[Answer]:a

## Q4. 마감 인원 판정 기준(정원 대비 인원)

백엔드 정원 체크는 `status=APPLIED` 인원만 셉니다(취소분 제외). 표시용 마감 판정도 동일 기준으로 맞출까요?

- A. 예 — APPLIED 인원 기준으로 `enrolledCount`/`full` 판정 (백엔드 정원 체크와 일치)
- X. Other (please specify)

[Answer]:a

---

## Consolidated Summary Confirmation

답변 요약:

- **Q1 = A** — Bug 2는 백엔드까지 완전 수정: `MeetingSummary`에 `enrolledCount` + `full` 추가, `listRecruiting`에서 모임별 APPLIED 인원 집계, OpenAPI 계약 + 백엔드 테스트 갱신, FE는 "마감" 배지 + 버튼 비활성.
- **Q2 = A** — 로드 시 이미 신청한 모임은 "신청완료" + 비활성(신청 직후와 동일 문구).
- **Q3 = A** — 정원이 찬 미신청 모임은 "마감" 배지 + "마감" 라벨의 비활성 버튼.
- **Q4 = A** — 마감 판정은 `status=APPLIED` 인원 기준(백엔드 정원 체크와 일치).

추가 확정 사항(위 답변에서 파생, 이견 없으면 그대로 반영):
- Bug 1의 내 신청 상태는 `enrollmentsApi.listMine()`으로 로드하며, 로그인한 MENTEE일 때만 조회한다(모집 목록 자체는 비로그인 공개).
- 이미 신청 + 정원 마감이 동시일 때는 "신청완료"(내 신청 상태)를 우선 표시한다.

Does this all look correct before I generate the requirements artifact?

- Looks correct — requirements.md 생성으로 진행
- Request changes — 수정할 항목 지정

[Answer]:Looks correct
