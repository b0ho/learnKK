# Domain Entities — U8 Survey/Feedback (learnKK / 런크크)

<!-- functional-design 산출물(architect 리드 + developer 기술 검토). Unit=U8 Survey/Feedback(kind=service). 스토리: US-3.6/8.1/8.2(unit-of-work-story-map.md). 출처: unit-of-work.md(U8=C7 사전설문 응답 ②후·과정 설문·피드백 열람; 문항 틀은 U3 read·응답은 U8 소유), requirements.md(FR3.6 ②후 응답·FR8.1/8.2), components.md(C7·소유 데이터 survey_answer/feedback), component-methods.md(PreSurveyService/FeedbackService), services.md, U1(ErrorPayload·Principal). 문항 틀=U3, 참여자=U4 read. Entity API 비노출(NFR8). -->

## 개요

U8은 C7(사전설문 응답·과정 설문·피드백) 도메인 엔티티 `survey_answer`·`feedback`를 소유한다. 사전설문 **문항 틀(SurveyQuestion)**은 U3 소유(read), **응답**은 U8. 응답 수집은 ②시작 이후 게이팅(FR3.6).

## 엔티티

### SurveyAnswer (사전설문 응답)

US-3.6. 멘티가 ②시작 후 멘토 구성 문항에 응답. 문항은 U3 read.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | U3 모임 |
| `questionId` | BIGINT (FK→survey_question) | NOT NULL | U3 문항 틀 참조 |
| `menteeId` | BIGINT (FK→user) | NOT NULL | 응답자 |
| `answerText` | text | | 응답(선택형은 선택값) |
| `createdAt` | timestamptz | | |

- `unique(question_id, mentee_id)` — 문항당 멘티 1응답(재제출은 갱신 [assumption]).
- 문항 틀(survey_question)은 U3 소유 — U8은 questionId로 참조·read.

### Feedback (과정 설문 / 피드백)

US-8.1/8.2. 멘티가 과정 종료·진행 중 피드백 제출, 멘토·관리자 열람.

| 속성 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | BIGINT (PK) | identity | |
| `meetingId` | BIGINT (FK→meeting) | NOT NULL | |
| `menteeId` | BIGINT (FK→user) | NOT NULL | 제출자 |
| `content` | text/json | NOT NULL | 과정 설문 응답(문항 구조 [assumption]) |
| `createdAt` | timestamptz | | |

- 과정 설문 문항 구조 [assumption]: 파일럿은 자유 서술 또는 고정 문항 셋 — 확정은 team([open]).

## 관계·통합 지점 (읽기 교차참조)

- `meetingId` → meeting(U3): 모임 상태(②시작 이후=IN_PROGRESS 응답 게이팅) read. 소유 멘토(피드백 열람 권한) read.
- `questionId` → survey_question(U3): 사전설문 문항 틀 read(getQuestions). U8은 문항을 소유하지 않음.
- **참여자 판정:** 응답·피드백 제출은 참여 멘티(U4 APPLIED read)만. U8 depends_on U4(DAG) — 정방향 비순환.
- **read-out:** 사전설문 응답(getAnswers)은 U3 운영 허브 화면이 read 조합(FE 조합, U3→U8), U9 모니터링도 참조 가능.

## 생명주기

- SurveyAnswer: ②시작 후 제출(그 전 비노출/거부). Feedback: 진행/완료 참여 멘티 제출.

## Assumptions & Open Questions

- **[assumption]** 문항당 1응답(재제출 갱신), 과정 설문 문항 구조(자유서술/고정셋).
- **[decided]** 사전설문 응답은 ②시작(IN_PROGRESS) 이후만(FR3.6 rev-us). 문항 틀=U3, 응답=U8.
- **[open]** 과정 설문 문항 구조 확정, U3 상태·문항 read 포트·U4 참여자 read 포트 시그니처.
