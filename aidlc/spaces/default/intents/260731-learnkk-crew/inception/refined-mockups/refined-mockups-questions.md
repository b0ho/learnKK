# Refined Mockups — 인터뷰 질문지 (learnKK / 런크크)

rough-mockups 9화면 와이어프레임과 user-flow가 이미 있고, requirements/stories(rev2)로 상세가 확정돼 있습니다. 아래는 고충실도 정교화에 필요한 **실제 설계 결정**만 담았습니다. 특히 rough-mockups는 rev2 이전 모델이라, 그 갱신 방향을 확인합니다.

각 `[Answer]:`에 보기 문자. 복수 선택 문항 표시. 직접 서술은 `X. 기타`.

---

## Q1. 충실도(fidelity) 수준
- A. (권장) **중~고충실도** — 화면별 레이아웃·컴포넌트·상태(로딩/빈/오류/성공)를 ASCII/구조 명세로 상세화(실제 픽셀 디자인 툴 산출물은 아님, 설계 전용)
- B. 저충실도 유지 + 상호작용 명세만 보강
- C. 고충실도 + 디자인 토큰(색·타이포·간격)까지 정의
- X. 기타

[Answer]:a

## Q2. 디자인 시스템 / 컴포넌트 라이브러리
React 프론트엔드 전제입니다.
- A. (권장) **경량 디자인 시스템을 자체 정의**(토큰 + 기본 컴포넌트 명세) — 외부 UI 라이브러리 강제하지 않음, 구현 워크플로우에서 선택 여지
- B. 특정 라이브러리 전제(예: MUI/Chakra/Ant) — 이름 지정
- C. 헤드리스(Radix/Headless UI) + 자체 스타일
- X. 기타

[Answer]:shadcn, 불가시 유사 라이브러리로 탐색

## Q3. rough-mockups의 rev2 반영 (변경 화면)
rev2로 바뀐 지점을 refined mockups에 어떻게 반영할까요? (해당되는 것 모두 선택 가능)
- A. (권장) **가입 화면에 사번 필드 추가**(화면 9)
- B. **신청과 사전설문 분리** — 신청(선착순, 화면 2 CTA)은 즉시, 사전설문 응답 화면은 **②시작 이후** 진입점으로 이동(화면 3 재배치)
- C. **멘토 화면(5)의 "모임 시작/종료 출석창"을 "세션 일정 지정·변경(주차당 복수)"로 교체**, 멘티 출석은 **예정 세션 시간 팝업 self check-in**(신규 화면)
- D. **출석률 표기를 세션 기준**(출석 세션/전체 예정 세션)으로 통일(화면 4·6)
- X. 기타

[Answer]:a

## Q4. 화면 상태(state) 커버리지
각 화면이 다뤄야 할 상태를 어디까지 명세할까요?
- A. (권장) **로딩·빈(empty)·오류(재시도)·성공**을 주요 화면마다 명세(CC-3 상속)
- B. 핵심 상태(로딩·오류)만
- C. 정상 상태만(상태는 functional-design 이월)
- X. 기타

[Answer]:a

## Q5. 반응형 브레이크포인트
모바일 웹뷰 우선입니다.
- A. (권장) **모바일 세로 단일 기준**(대표 폭 360~430px)으로 설계, 태블릿/데스크톱은 범위 밖(확대 대응만 언급)
- B. 모바일 + 태블릿 2단계
- C. 모바일/태블릿/데스크톱 3단계
- X. 기타

[Answer]:a

## Q6. 접근성 체크리스트 수준 (NFR7 = 기본 준수, WCAG 인증 목표 아님)
- A. (권장) **기본 준수 체크리스트** — 폼 라벨, 색+텍스트 병기, 키보드/포커스 순서, 터치 타겟, 한국어 스크린리더 라벨, 랜드마크/헤딩 구조 (WCAG 2.1 A/AA 항목 중 상식 수준 매핑, 인증은 목표 아님)
- B. WCAG 2.1 AA 전 항목 체크리스트
- C. 최소 — 폼 라벨·대비만
- X. 기타

[Answer]:a

## Q7. 상태 어휘 통일 (rough-mockups 리뷰 지적)
화면1 필터 [모집중][진행중][마감][종료]와 라이프사이클 [개설신청→모집중→시작대기→진행중→완료/반려/취소]가 어긋납니다.
- A. (권장) **상태머신 canonical 어휘로 통일**(개설신청/모집중/시작대기/진행중/완료/반려/취소) + 목록 필터는 사용자 친화 라벨을 canonical에 매핑해 표기
- B. 목록 필터 어휘를 그대로 두고 매핑표만 제공
- X. 기타

[Answer]:a

---

## Follow-up FQ1 (Q3 명확화) — 필수 정합성
Q3에 A(사번 필드)만 선택하셨습니다. 그런데 B/C/D는 이미 **승인된 stories(rev2)**의 내용이라, refined mockups가 stories와 정합하려면 함께 반영되어야 합니다(A만 반영 시 mockups가 승인 stories와 모순 — 멘토 출석창·신청 시 설문 등 구모델이 남음). 어떻게 할까요?
- A. (권장) A+B+C+D 모두 반영 — mockups를 승인된 rev2 stories와 완전 정합화(신청/설문 분리, 세션 일정 UI, 팝업 출석, 세션 분모 표기 포함)
- B. A만 반영하고 B/C/D는 의도적으로 제외 — (이 경우 mockups는 rev2 stories와 불일치하게 되며, 그 사유를 명시해야 함)
- X. 기타(직접 서술)

[Answer]: A. A+B+C+D 모두 반영

<!-- Consolidated Summary Confirmation (filled after all answers collected) -->
## Consolidated Summary Confirmation

정리된 답변:
- Q1 = A — 중~고충실도(레이아웃·컴포넌트·상태 명세, 설계 전용).
- Q2 = **shadcn/ui**(불가 시 유사 라이브러리 탐색) — Radix + Tailwind 계열 헤드리스 컴포넌트.
- Q3 + FQ1 = A+B+C+D 모두 — 사번 필드(화면9), 신청/사전설문 분리(신청 즉시 + 설문 ②후), 멘토 세션 일정 지정·변경(주차당 복수) + 멘티 팝업 출석(신규 화면), 출석률 세션 기준 표기.
- Q4 = A — 로딩·빈·오류(재시도)·성공 상태 주요 화면마다 명세.
- Q5 = A — 모바일 세로 단일 기준(360~430px).
- Q6 = A — 기본 접근성 체크리스트(WCAG 인증 목표 아님).
- Q7 = A — 상태머신 canonical 어휘 통일 + 필터 라벨 매핑.

프롬프트: "이대로 refined mockups 산출물(mockups/interaction-spec/design-system-mapping/accessibility-checklist)을 생성해도 될까요?"
- A. Looks correct — 생성 진행
- B. Request changes — 일부 답변 수정

[Answer]: A. Looks correct

---

<!-- §13 Learnings Ritual — pending human turn (blank [Answer] marks genuine human-wait for the Stop hook) -->
## Learnings Ritual
프롬프트: "surface된 후보(c1~c3) 중 harness에 남길 항목을 고르고, 다음을 위해 추가할 메모가 있습니까?"
후보: c1(상류 와이어프레임이 구모델이면 현재 승인 stories 기준으로 갱신), c2(승인분과 모순되는 under-answer는 추측 말고 되물어 정합화), c3(리뷰 수정도 diff 위생 규칙대로 타깃 편집) — 각 `→ project.md`, team.md 승격 가능.
- 1. 아무것도 남기지 않음
- 2. 후보 선택 (남길 번호 지정; team 승격 여부 포함)
- 3. 메모 추가 (자유 서술 + diary 헤딩 선택)

[Answer]: 1. 아무것도 남기지 않음
