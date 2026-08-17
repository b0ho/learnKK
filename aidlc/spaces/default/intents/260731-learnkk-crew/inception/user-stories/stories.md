# User Stories — learnKK (런크크)

<!-- user-stories 리드(product-agent) 초안 → mob(design/developer/quality) 통합본 → product-lead 리뷰 반영 → 게이트 cycle2 [rev2] 최소-diff 반영. 출처: requirements.md(FR/NFR), personas.md, project.md Decided. 인수기준은 inception 규칙에 따라 Given/When/Then(BDD). MoSCoW: Must/Should/Could/Won't. 인수기준의 테스트 가능성·경계값은 team-practices의 Testing Posture(80% coverage·계약 테스트)를 상속해 검증한다. [rev2]=게이트 cycle2 사용자 수정(사번·사전설문 ②후·세션 일정/팝업 출석). -->

계획(전부 A): 여정 기반 에픽 ①~⑨, 중간 세분화(행위 1개=스토리 1개), 핵심 여정 전체 Must, 관리자 4지점 승인 각각 독립 스토리, 미확정(OQ1/OQ2/FR9.2)은 노트/가정 이월. INVEST 준수. 사용자 노출 텍스트 한국어(team-practices Code Style 상속). mob 통합으로 인증/현황 스토리 신설, 과대 스토리 분리, 테스트가능성·접근성 보강.

## 횡단 인수기준 (Cross-cutting AC — 모든 해당 스토리가 상속)

- **CC-1 에러 계약(NFR8)**: 임의의 오류 응답에 대해 — 본문은 `{code,message,details}` 스키마를 따르고 상태코드는 규약(검증 400 / 인증 401 / 인가 403 / 미존재 404 / 상태전이 위반 409)을 준수한다. 각 negative AC는 이를 상속하며 OpenAPI 계약 테스트로 검증(team-practices). [quality C9]
- **CC-2 접근성·모바일(NFR1/NFR7)**: 모든 화면 스토리는 — 폼 입력에 라벨 연결, 상태/뱃지는 색상+텍스트(또는 아이콘) 병기(색상 단독 의존 금지), 키보드/포커스 이동 가능, 모바일 터치 타겟 확보, 한국어 스크린리더 라벨을 기본 지향(공식 WCAG 인증 목표 아님). [design D]
- **CC-3 목록 상태**: 모든 목록형 화면은 빈 상태(안내 문구+다음 행동), 로딩 지연 표시, 실패 시 재시도 안내를 갖는다. [design D]

## 모임 상태머신 (도메인 타입 계약 #3 — 단일 canonical 정의)

<!-- Text fallback: 개설신청 → (①승인) 모집중 → (모집확정) 시작대기 → (②승인) 진행중 → (③모임완료 관리자 직접 + ④멘티수료 승인) 완료. 개설신청에서 반려로, 모집중/시작대기에서 취소로 분기. 반려·취소는 종료 상태. -->

```
[개설신청] --①승인--> [모집중] --모집확정--> [시작대기] --②승인--> [진행중] --③모임완료(관리자 직접) & ④멘티수료--> [완료]
    |                     |
    +--①반려--> [반려]     +--정원미달 취소/멘티전원취소--> [취소]
```

- 상태: 개설신청 / 모집중 / 시작대기 / 진행중 / 완료 / 반려(종료) / 취소(종료).
- 멘티 수료 상태(모임과 별개): 미수료 / 수료후보(≥80% 자동) / 수료확정(④승인).
- **진행중 단계의 순서 [rev2]**: ②시작 승인 후 — 멘티 사전설문 응답(US-3.6) + 멘토 세션 일정 지정/공유(US-6.2) + 세션 시간에 멘티 팝업 출석(US-6.3).
- 이 enum·전이표는 **공유 도메인 타입 계약(team-practices 계약 #3)**의 소유 정의다. US-2.2/US-3.4/US-6.1/US-7.2/US-7.3가 이를 참조한다. 불법·이중·역순 전이는 409로 거부(CC-1). 상세 전이 규칙(반려 후 재신청 경로 등)은 functional-design에서 확정. [developer #3/#4, quality C5]

## Epic ① 계정·프로필·인증 (FR1)

### US-1.1 회원가입 (Must)
- As a 멘티/멘토, I want 닉네임·비밀번호·**사번**으로 가입, so that 별도 승인 대기 없이 바로 이용한다. [rev2]
- 인수기준:
  - Given 미가입 방문자, When 고유 닉네임+비밀번호+사번으로 가입 제출, Then 승인 절차 없이 계정이 생성되고 로그인 상태가 된다.
  - Given 이미 사용 중인 닉네임 또는 **이미 등록된 사번**, When 가입, Then 중복 오류가 반환된다(CC-1).
  - Given 비밀번호 저장, Then bcrypt 해시로 저장된다(DB에 평문 없음).
- 노트: 사번 형식·유일성 범위는 functional-design 확정(A5). [rev2]
- 의존성: 없음(기반). Ref: FR1.1, FR1.2, NFR6.

### US-1.2 로그인·세션·RBAC 경계 (Must) [신설: developer #3 / design B]
- As a 사용자, I want 재접속 시 로그인하고 세션이 유지되며 역할에 맞는 접근만 허용된다, so that 안전하게 다시 이용한다.
- 인수기준:
  - Given 기존 사용자, When 닉네임+비밀번호로 로그인, Then 인증되어 세션이 발급되고 홈으로 진입한다.
  - Given 잘못된 자격정보, When 로그인, Then 계정 존재 여부를 특정하지 않는 표준 인증 오류(401, CC-1)가 반환된다.
  - Given 만료/무효 세션, When 보호 리소스 접근, Then 401로 재로그인이 요구된다.
  - Given 역할(멘토/멘티/관리자) 밖의 액션, When 호출, Then 403으로 거부된다(FR1.5 RBAC).
  - Given 로그아웃, When 실행, Then 세션이 무효화된다.
- 의존성: US-1.1. 노트: walking skeleton 공통 기반의 "인증"이자 API 계약 #1의 인증/인가 엔드포인트를 핀한다(team-practices). Ref: FR1.5, NFR6/NFR8.

### US-1.3 사번 기반 중복계정 방지 (Must) [rev2 — 기존 히든 IP 신호 대체]
- As a 시스템, I want 사번 유일성으로 중복계정을 방지, so that 한 사람이 다수 계정을 만들지 못하게 한다.
- 인수기준:
  - Given 이미 등록된 사번, When 동일 사번으로 재가입 시도, Then 거부된다(CC-1).
  - Given 가입 요청, Then 사번 유일성 제약(DB 유니크)이 강제된다.
- 노트[rev2]: 기존 히든 IP salted-hash/보관창 방식은 폐기(NFR5 갱신, OQ5 해소). project.md Decided의 히든 안티-중복 결정을 사용자 게이트 수정으로 대체 — reconcile 필요.
- 의존성: US-1.1. Ref: FR1.4.

### US-1.4 프로필 작성·조회 (Must)
- As a 멘티/멘토, I want 관심사 해시태그와 한 줄 소개를 프로필에 남긴다, so that 나를 소개한다.
- 인수기준:
  - Given 로그인 사용자, When 관심사 해시태그·한 줄 소개 저장, Then 프로필에 반영·조회된다.
  - Given 빈 값/초과 길이, When 저장, Then 검증 오류(400, CC-1)가 반환된다. (CC-2 상속)
- 의존성: US-1.1. Ref: FR1.3.

## Epic ② 모임 개설·승인 (FR2)

### US-2.1a 멘토 모임 기본정보 개설 (Must) [분리: developer #1]
- As a 멘토, I want 주제·학습기간·모집기간·정원·진행방식·초기 자료/공지로 모임을 개설, so that 러닝 크루를 시작한다.
- 인수기준:
  - Given 로그인 멘토, When 필수 항목으로 개설 제출, Then 모임이 **개설신청** 상태로 생성된다.
  - Given 학습기간 입력, Then 주차 구조의 기준이 확정된다(구체 세션 일정은 US-6.2에서 ②시작 후 지정). [rev2] (OQ6 해소: 주차=세션 일정 기반)
- 의존성: US-1.2. Ref: FR2.1, FR6.1.

### US-2.1b 사전설문 문항 빌더 (Must) [분리: developer #1]
- As a 멘토, I want 신청 사전설문 문항을 자유롭게 구성(추가/삭제/순서), so that 신청자에게 받을 정보를 정한다.
- 인수기준:
  - Given 개설/수정 중 모임, When 설문 문항 구성·저장, Then 설문 틀로 확정된다(응답은 US-3.6에서 ②시작 후 수집). [rev2]
  - Given 문항 0개, When 저장, Then 설문 없는 모임으로 유효 처리된다(또는 최소 1문항 규칙 — functional-design 확정).
- 의존성: US-2.1a. Ref: FR2.1.

### US-2.2 관리자 개설 승인 ① (Must)
- As a 시스템 관리자, I want 개설신청 모임을 승인/반려, so that 정식 모임 전환 여부를 통제한다.
- 인수기준:
  - Given 개설신청 상태, When 관리자 승인(①), Then 상태가 **모집중**으로 전환된다.
  - Given 관리자 반려, When 반려 사유 제출, Then **반려(종료)** 상태가 되고 사유가 멘토 운영 허브(US-2.3)에서 텍스트로 열람된다. [design C]
  - Given 개설신청이 아닌 상태, When ①승인 시도, Then 409로 거부된다(CC-1, 상태머신).
- 의존성: US-2.1a. Ref: FR2.2, FR2.3.

### US-2.3 멘토 운영 허브 — 내 모임 + 신청자·설문 응답 열람 (Must) [신설: design B]
- As a 멘토, I want 내 모임 상태·세션 일정·신청자 명단과 (②시작 후) 사전설문 응답을 한 화면에서 본다, so that 운영을 파악·진행한다.
- 인수기준:
  - Given 멘토, When 내 모임 조회, Then 모임별 상태·모집현황·세션 일정·세션 진행 현황과 반려 사유(있으면)가 표시된다. (CC-2/CC-3 상속)
  - Given ②시작 후 응답 존재, When 신청자 상세 열람, Then 각 멘티의 사전설문 응답이 표시된다.
  - Given 타 멘토 모임, When 열람 시도, Then 403으로 거부된다(CC-1).
- 의존성: US-2.1a, US-3.2, US-3.6. Ref: FR2.1, FR3.2, FR3.6.

## Epic ③ 탐색·모집·신청 (FR3)

### US-3.1 모임 목록 탐색 (Must)
- As a 멘티, I want 모집중 모임 목록을 탐색, so that 관심 모임을 찾는다.
- 인수기준:
  - Given 모집중 모임 존재, When 목록 조회, Then 주제·기간·정원·모집현황과 함께 표시된다.
  - Given 정원 마감 모임, Then "모집마감" 텍스트 라벨 + 신청 버튼 비활성으로 표시(색상 단독 의존 금지, CC-2). [design C]
  - (CC-3 빈/로딩/오류 상태 상속)
- 노트: 응답시간 목표(NFR3, 비게이팅)는 performance-validation에서 부하·백분위로 검증 — 기능 AC에서 제외. [quality C1]
- 의존성: US-2.2. Ref: FR3.1.

### US-3.2 모임 신청 (선착순) (Must) [rev2 — 설문 응답 분리]
- As a 멘티, I want 모임에 신청, so that 참여 의사를 등록한다.
- 인수기준:
  - Given 모집중 & 정원 여유, When 신청 제출, Then 선착순 접수된다(**사전설문은 이 시점에 받지 않음** — US-3.6에서 ②시작 후 수집).
  - Given 정원 N·현재 N-1건(잔여 1석), When 두 멘티가 동시 신청, Then 정확히 1건만 접수되고 나머지는 마감 안내(초과 접수 0). [quality C2]
  - Given 이미 신청한 멘티, When 재신청, Then 중복 신청으로 거부(정원 이중 점유 없음, CC-1). [quality C2]
- 의존성: US-3.1. Ref: FR3.2, FR3.3.

### US-3.3 신청 취소 (Must)
- As a 멘티, I want 신청을 취소, so that 참여를 철회한다.
- 인수기준:
  - Given 모집중 신청 상태, When 취소, Then 철회되고 정원 여유가 복원된다.
  - Given 모집확정 후 **시작대기(② 이전)**, When 취소, Then **허용**된다(FR3.5 "시작 이후 불가"의 반대 해석). [quality C3]
  - Given 모임 시작(②) 이후, When 취소 시도, Then 거부된다(이후 출석율로만 판정).
- 노트[판단콜/이월]: FR3.5 문언은 "모집 기간 중" 취소를 명시 — 시작대기(모집확정 이후) 취소 허용은 반대해석이므로 functional-design에서 사용자 확정. [reviewer S4]
- 의존성: US-3.2. Ref: FR3.5.

### US-3.4 모집 마감 확정 (Must) [승격 Should→Must: developer #5 — 상태머신 크리티컬 패스, 인간 Q3=A 정합]
- As a 시스템 관리자, I want 모집 마감 시 모집을 확정하고 미달 시 진행/취소를 결정, so that 시작 여부를 통제한다.
- 인수기준:
  - Given 모집기간 종료, When 관리자가 모집 확정, Then 모임이 **시작대기**가 된다.
  - Given 정원 미달, When 진행 결정, Then 시작대기; 취소 결정, Then **취소(종료)** 상태로 전환된다.
  - Given 모집중이 아닌 상태, When 모집 확정 시도, Then 409로 거부(CC-1).
- 노트[이월]: 이 액션을 정식 5번째 승인 지점으로 편입할지는 functional-design 정합화(OQ1). **시작대기 전이는 하드 선행조건 — MVP에서 드롭 불가.** [developer #5]
- 의존성: US-3.2. Ref: FR3.4, OQ1.

### US-3.5 멘티 "내 신청/내 모임" 현황 (Must) [신설: design B]
- As a 멘티, I want 신청·참여 모임의 상태와 **세션 일정**을 한 화면에서 확인, so that 푸시 없이도 내 상황을 파악한다.
- 인수기준:
  - Given 신청 이력 보유, When 현황 조회, Then 신청/참여 모임이 상태(접수/①승인/반려/모집확정/시작/취소)·다음 액션(설문 응답 필요·출석 예정 세션 등)과 함께 표시된다(CC-2/CC-3).
  - Given ②시작 후 세션 일정 공유/변경, When 조회, Then 각 세션의 날짜·시간이 표시된다(변경분 반영). [rev2]
  - Given 모임이 정원 미달로 취소(US-3.4), When 조회, Then "취소됨"으로 표시된다.
- 의존성: US-3.2, US-6.2. Ref: FR3.2, FR3.5, FR6.1 (푸시 없음 FR5.2 함축).

### US-3.6 멘티 사전설문 응답 (②시작 후) (Must) [rev2 — 신청과 분리]
- As a 멘티, I want ②시작 승인 이후 멘토가 구성한 사전설문에 응답, so that 멘토가 참여자 정보를 파악한다.
- 인수기준:
  - Given ②시작 승인된 모임의 참여 멘티, When 사전설문 응답 제출, Then 응답이 저장되고 멘토·관리자가 열람 가능(US-2.3).
  - Given ②시작 이전, When 설문 응답 시도, Then 아직 개시되지 않음으로 거부/비노출(CC-1).
- 노트[rev2]: "사전(pre)" 명칭이나 실제 응답 시점은 ②시작 이후(사용자 결정). 미응답자 처리(리마인드·필수 여부)는 functional-design(OQ7).
- 의존성: US-2.1b, US-6.1(②승인). Ref: FR3.6.

## Epic ④ 주차 진행 — 게시글형 자료실·공지 (FR4)

### US-4.1a 주차 게시글 본문 작성·조회 (Must) [분리: developer #2]
- As a 멘토, I want 주차별 게시글을 본문 텍스트로 작성, so that 첨부 없이도 자료·설명을 남긴다.
- 인수기준:
  - Given 시작대기/진행중 모임, When 본문만으로 게시글 작성, Then 첨부 없이 저장·조회된다.
  - Given 게시글 저장, Then 본문+메타데이터(작성자·주차·작성시각)가 저장된다.
- 의존성: US-2.2. Ref: FR4.1, FR4.2.

### US-4.1b 게시글 파일 첨부 (Must) [분리: developer #2]
- As a 멘토, I want 게시글에 파일을 첨부(0개 이상), so that 학습 자료 파일을 제공한다.
- 인수기준:
  - Given 게시글, When 파일 첨부, Then PostgreSQL(BLOB) 저장 + 파일 메타데이터(파일명·형식·크기·업로더·소속 게시글/주차) 관리.
  - Given 화이트리스트 외 형식, When 업로드, Then 형식 불허 오류(400, CC-1). [quality C7]
  - Given 상한 초과 파일, When 업로드, Then 크기 초과 오류(CC-1). (상한 정확값/+1 경계 테스트는 OQ4 확정 후) [quality C7]
- 노트: BLOB 업/다운로드는 스트리밍 처리(멀티파트 max-size 정렬)로 OOM 회피 — functional-design/구현 확정. 형식 화이트리스트·최종 상한 OQ4. [developer #6]
- 의존성: US-4.1a. Ref: FR4.3, FR4.4.

### US-4.2 자료 게시글 열람 (Must)
- As a 멘티, I want 주차 게시글과 첨부를 열람, so that 학습 자료를 확인한다.
- 인수기준:
  - Given 참여 중인 모임의 게시글, When 열람, Then 본문·첨부 목록이 표시되고 첨부를 내려받을 수 있다.
  - Given 해당 모임 비참여자, When 열람 시도, Then 403으로 거부된다(CC-1). [quality C8]
- 의존성: US-4.1a, US-3.2. Ref: FR4.1~FR4.4.

### US-4.3 공지 게시 (Must)
- As a 멘토, I want 공지사항을 게시, so that 중요한 안내를 전달한다.
- 인수기준:
  - Given 진행 모임, When 공지 게시, Then 멘티 목록·뱃지에 반영된다(뱃지는 텍스트 대체 병기, CC-2).
- 의존성: US-2.2. Ref: FR4.5.

## Epic ⑤ 쪽지 (FR5)

### US-5.1 쪽지 주고받기 (Must)
- As a 멘토/멘티/관리자, I want 상대와 쪽지를 주고받는다(멘토↔멘티, 관리자↔멘토/멘티), so that 필요한 소통을 한다.
- 인수기준:
  - Given 권한 있는 상대, When 쪽지 전송, Then 수신자의 인앱 목록·미확인 뱃지에 반영(폴링/새로고침, 푸시 없음; 뱃지 텍스트 대체 "안 읽은 쪽지 N건" 병기, CC-2). [design C]
  - Given 멘토가 자기 모임에 속하지 않은 멘티, When 전송, Then 403으로 거부(관리자는 전원 허용, CC-1). [quality C8]
- 노트[이월]: 난이도·아키텍처 영향이 낮으면 채팅형 전환 검토(OQ2). 기본 가정=인앱 쪽지.
- 의존성: US-1.2. Ref: FR5.1~FR5.3.

## Epic ⑥ 세션 일정·출석 (FR6)

### US-6.1 관리자 모임 시작 승인 ② (Must)
- As a 시스템 관리자, I want 시작대기 모임의 시작을 승인, so that 모임이 진행 단계로 들어간다.
- 인수기준:
  - Given 시작대기 모임, When 관리자 시작 승인(②), Then 상태가 **진행중**이 되어 세션 일정 지정·사전설문 응답·출석이 가능해진다.
  - Given 시작대기가 아닌 상태, When ②승인 시도, Then 409로 거부(CC-1). [quality C5]
- 의존성: US-3.4. Ref: FR2.3, FR6.1.

### US-6.2 멘토 세션 일정 지정·변경·공유 (Must) [rev2 — 출석창 개폐에서 변경]
- As a 멘토, I want 주차별 수업 세션의 날짜·시간을 지정하고(주차당 복수 세션 가능) 유동적으로 변경, so that 멘티에게 모임 일정을 알린다.
- 인수기준:
  - Given ②시작된(진행중) 모임, When 멘토가 세션의 날짜·시간을 지정, Then 세션이 생성되어 멘티에게 공유된다(US-3.5에 반영).
  - Given 한 주차, When 멘토가 세션을 여러 개 추가, Then 복수 세션이 등록된다.
  - Given 기존 세션, When 멘토가 날짜·시간 변경, Then 변경분이 멘티 현황에 반영된다.
- 노트: 과거 세션 편집 제약·변경 통지 상세 functional-design(A6). 로컬 단일 인스턴스에서 시간 비교로 처리(무거운 스케줄러 불요). [developer #6]
- 의존성: US-6.1(②승인). Ref: FR6.1.

### US-6.3 멘티 팝업 출석 체크 (세션별) (Must) [rev2 — 팝업 self check-in]
- As a 멘티, I want 예정 세션 시간에 시스템 팝업으로 직접 출석 체크, so that 내 참여가 세션별로 기록된다.
- 인수기준:
  - Given 예정 세션 시간이 도래, When 참여 멘티가 접속, Then 시스템이 출석 팝업을 제공하고 멘티가 직접 체크하면 해당 세션 출석 1회 기록(중복 체크는 멱등).
  - Given 세션 출석 시간(창) 밖, When 출석 시도, Then 거부된다.
  - Given 비참여자, When 출석 시도, Then 거부(self check-in 주체=참여 멘티, CC-1). [quality C11]
- 노트: 출석 유효 시간창(세션 시각 기준 허용 범위)은 functional-design 확정.
- 의존성: US-6.2. Ref: FR6.2.

## Epic ⑦ 수료·완료 (FR7)

### US-7.1 출석율 산출·수료 자동 판정 (Must) [rev2 — 분모=전체 예정 세션]
- As a 시스템, I want 각 멘티의 출석율을 **전체 예정 세션 수** 대비로 산출해 80% 이상을 수료후보로 자동 판정, so that 판정을 자동화한다.
- 인수기준:
  - Given 전체 예정 세션 S·출석 세션 a, When 산출, Then 수료후보 판정은 **(a/S) ≥ 0.80** 이며 부동소수 오차를 피하는 정수 비교(**a*100 ≥ 80*S**)로 고정한다. [quality C4]
  - Given S=5,a=4(정확히 80%), Then 충족. / Given S=5,a=3(60%), Then 미충족. / Given S=6,a=5(83.3%), Then 충족. [quality C4]
- 노트: 세션이 진행 중 추가/변경되면 S가 바뀔 수 있음 — 판정 시점의 확정 세션 집합 기준(functional-design 확정). [rev2]
- 의존성: US-6.3. Ref: FR6.3, FR7.1.

### US-7.2 관리자 멘티 수료 승인 ④ (Must)
- As a 시스템 관리자, I want 자동 판정된 수료후보를 승인, so that 멘티 수료를 확정한다.
- 인수기준:
  - Given 수료후보(≥80%), When 관리자 승인(④), Then 멘티 수료가 확정된다.
  - Given 미충족(<80%), Then 승인 대상에 오르지 않는다.
  - Given 진행중이 아닌(예: 미완료 주차 잔존) 상태의 승인 시도 또는 이미 확정된 수료의 재승인, When 실행, Then 409로 거부된다(CC-1, 상태머신). [reviewer S5]
- 의존성: US-7.1. Ref: FR7.1.

### US-7.3 관리자 모임 완료 처리 ③ (Must) [rev3 — 멘토 완료 인정 신청 제거]
- As a 시스템 관리자, I want 전 세션이 종료된 모임을 직접 완료 처리(③), so that 멘토 신청 단계 없이 모임 정상 완료를 확정한다.
- 인수기준:
  - Given 예정 세션 전부 종료된 진행중 모임, When 관리자가 완료 처리(③), Then 모임이 정상 완료(③) 상태가 된다.
  - Given 세션 미완료 상태의 ③ 처리 시도, 또는 이미 ③처리된 건의 재처리, When 실행, Then 409로 거부된다(CC-1, 상태머신). [reviewer S5]
  - Given 멘토, When 멘티 수료 판정을 시도, Then 권한 없음(403)으로 거부된다(권한 경계). [quality AGREE]
- 노트[rev3]: 멘토의 "완료 인정 신청" 단계 폐지 — 멘토는 세션 운영·자료·피드백 확인만 담당. project.md Decided(멘토 완료 인정 신청) supersede — reconcile 필요.
- 의존성: US-6.2. Ref: FR7.2, FR7.3.

### US-7.4 멘티 현황·수료 결과 확인 (Must) [신설: design B]
- As a 멘티, I want 진행 중 내 출석 세션/전체 예정 세션과 80% 대비 현황, 최종 수료 여부를 확인, so that 목표(80% 수료)를 추적한다.
- 인수기준:
  - Given 진행 중, When 내 진행 조회, Then 출석 세션/전체 예정 세션과 80% 대비 현황이 표시된다(CC-2/CC-3).
  - Given ④수료 승인 완료, When 조회, Then "수료 확정"이 표시된다.
- 의존성: US-6.3, US-7.2. Ref: FR7.1(멘티측 가시성).

## Epic ⑧ 과정 설문·피드백 (FR8)

### US-8.1 멘티 과정 설문 제출 (Must)
- As a 멘티, I want 과정 설문(피드백)을 제출, so that 경험을 남긴다.
- 인수기준:
  - Given 진행/완료 모임 참여 멘티, When 설문 제출, Then 피드백이 저장된다.
  - Given 완료 시점, Then 설문 제출 진입점이 내 현황(US-3.5/US-7.4)에서 노출된다. [design C]
- 의존성: US-3.2. Ref: FR8.1.

### US-8.2 멘토·관리자 피드백 확인 (Must)
- As a 멘토(및 시스템 관리자), I want 멘티 피드백을 열람, so that 과정 품질을 파악한다.
- 인수기준:
  - Given 제출된 피드백, When 멘토 조회, Then 자기 모임 피드백을 열람한다.
  - Given 시스템 관리자, When 조회, Then 피드백을 열람할 수 있다.
  - Given 타 모임 멘토, When 조회 시도, Then 403(CC-1).
- 의존성: US-8.1. Ref: FR8.2.

## Epic ⑨ 관리자 모니터링 (FR9, cross-cutting)

### US-9.1 승인 큐 대시보드 (Must)
- As a 시스템 관리자, I want 4지점 승인 대기(①②③④) 큐를 한 화면에서 처리, so that 품질 게이트를 신속히 통과시킨다.
- 인수기준:
  - Given 각 승인 대기 건, When 대시보드 조회, Then ①개설 ②시작 ③모임완료 ④멘티수료 대기 건이 큐로 표시되고 각 항목에서 승인/반려로 이동할 수 있다(CC-3).
- 의존성: US-2.2, US-6.1, US-7.2, US-7.3. Ref: FR9.1.

### US-9.2 운영 현황 모니터링 (Must)
- As a 시스템 관리자, I want 전체 모임 목록·상태와 모임별 출석율·수료 진행을 조회, so that 전체 현황을 감독한다.
- 인수기준:
  - Given 다수 모임, When 모니터링 조회, Then 모임별 상태·출석율(세션 기준)·수료 진행이 표시된다(CC-3).
- 의존성: US-6.3, US-7.1. Ref: FR9.1.

### US-9.3 집계 지표 (Won't — 이번 설계)
- As a 시스템 관리자, I want 개설 대비 승인·모집 충족률·평균 출석율·수료율·만족도·재개설률 집계, so that 프로그램 성과를 평가한다.
- 상태: **Won't (이번 설계 스코프)** — FR9.2에 따라 TBD 이월. 후속에서 반영 시점·범위 결정(OQ3).
- Ref: FR9.2, OQ3.

## 우선순위 요약 (MoSCoW)

- **Must (30개)**: US-1.1~1.4(4), US-2.1a·2.1b·2.2·2.3(4), US-3.1~3.6(6), US-4.1a·4.1b·4.2·4.3(4), US-5.1(1), US-6.1~6.3(3), US-7.1~7.4(4), US-8.1~8.2(2), US-9.1~9.2(2) = 4+4+6+4+1+3+4+2+2 = 30. 핵심 여정 전체 + 인증·현황·세션·사전설문 응답.
- **Should**: 없음(US-3.4는 크리티컬 패스라 Must로 승격).
- **Could**: 없음.
- **Won't(이번 설계, 1개)**: US-9.3(집계 지표), 그리고 out-of-scope 항목(게이미피케이션·결제·SSO·프로필 고도화·리포트 상세화).
- 총 31개 스토리(Must 30 / Won't 1).

## Assumptions & Open Questions

- 미확정(노트 표기·하류 이월): 모집 마감 관리자 확정의 승인모델 편입(OQ1), 쪽지→채팅 전환(OQ2), 집계 지표 반영(OQ3/FR9.2), 첨부 형식·상한(OQ4), 사번 형식·유일성 범위(A5), 세션 변경 통지·과거 세션 편집 제약(A6), 사전설문 미응답자 처리·신청 흐름 세부(OQ7), 시작대기 취소 허용(US-3.3 판단콜), 역할 겸직(personas, application-design), 출석 유효 시간창. [rev2: OQ5(안티-중복 신호)·OQ6(주차 일정/순번) 해소]
- 상태머신·수료 상태 enum은 공유 도메인 타입 계약(#3)으로, DB 스키마 계약(#2, user[사번]/meeting/survey question·answer/post·attachment/session/attendance), API 계약(#1, US-1.2 인증)과 함께 delivery-planning/functional-design에서 owner 배정·고정.
- MVP 경계(어느 스토리까지)는 delivery-planning에서 확정하며 위 MoSCoW가 근거. US-3.4·US-1.2는 드롭 불가 하드 선행조건.

## Mob 통합 기록 (dissent)

- 3인(design/developer/quality) 기고는 모두 상호 보완적이며 리드가 전량 통합했다. maintained dissent(미해소 반대) 없음 — 모든 OBJECT가 신규 스토리·분리·테스트가능성 보강·상태머신 계약화로 반영됨.
- product-lead 리뷰 제안 반영: S1(카운트 정정), S2(US-3.5를 Epic③로 이동), S3(US-2.3/US-3.5 FR9 오참조 제거), S5(US-7.2/7.3 불법전이 negative 명시). S4(시작대기 취소)는 functional-design 사용자 확정으로 이월.
- **게이트 cycle2 사용자 수정 [rev2]**: (1) 회원가입 사번 추가 + 사번 기반 중복방지(히든 IP 대체, US-1.1/1.3), (2) 사전설문 응답을 ②시작 이후로 이동(US-3.2 신청 분리 + US-3.6 신설), (3) 주차별 세션 일정(멘토 지정·변경·공유, 복수 세션, US-6.2) + 멘티 팝업 self check-in(세션별, US-6.3) + 출석 분모=전체 예정 세션(US-7.1). requirements.md도 정합화([rev-us]). OQ5/OQ6 해소. 이 수정은 최소 diff(변경분만 타깃 편집)로 반영함.
- **refined-mockups 단계 사용자 수정 [rev3]**: 멘토 "완료 인정 신청" 단계 폐지 — 전 세션 종료 시 관리자가 ③(모임 정상 완료)를 직접 처리(US-7.3 개정). 4지점 승인 유지, ③ 트리거만 멘토신청→관리자 직접으로 변경. project.md Decided(멘토 완료 인정 신청)·requirements FR7.2 supersede·정합화([rev-mk]). 최소 diff 타깃 편집.


## Review

**Reviewer:** aidlc-product-lead-agent
Re-review type: POST-RECONSTRUCTION (diff-hygiene 재구성 — baseline 복원 + rev2 최소-diff 재적용, 회귀 검증)

Verdict: READY

재구성이 rev2 의미를 손실 없이 보존했음을 확인했다. 적대적 검토(회귀·의미 유실 가정 → 반증 시도)로도 blocking을 세우지 못했다. 이전 승인된 rev2 콘텐츠와 의미적으로 동일하며, gratuitous 리워딩 없이 최소 diff로 재적용됨. stale `## Review` 섹션은 제거됨(확인).

### Blocking (없음)

없음.

### 검증 근거 (통과 항목)

- **사번** — US-1.1: 닉네임+비밀번호+사번 가입, "이미 등록된 사번" 중복 오류(CC-1), A5 노트("사번 형식·유일성 범위는 functional-design 확정") 모두 present. US-1.3: 사번 유일성 제약(DB 유니크) 강제 AC, "히든 IP salted-hash/보관창 방식은 폐기(NFR5 갱신, OQ5 해소)" 노트 present. requirements FR1.4/NFR5/A4와 정합.
- **사전설문 ②후** — US-3.2: "선착순 접수 … 사전설문은 이 시점에 받지 않음(US-3.6에서 ②시작 후 수집)". US-3.6(신설): ②시작 승인 후 응답 저장·열람 AC + "②시작 이전 응답 시도 거부/비노출" negative AC, deps US-2.1b/US-6.1. US-2.1b 노트: "응답은 US-3.6에서 ②시작 후 수집". US-2.3 deps에 US-3.6 포함, "②시작 후 응답 열람" AC present.
- **세션** — Epic⑥ 제목 "세션 일정·출석"로 retitle. US-6.2: 멘토 세션 일정 지정·복수 세션·유동 변경·공유 3개 AC. US-6.3: 예정 세션 시간 팝업 self check-in(세션별, 멱등), 창 밖/비참여자 거부. US-2.1a: "주차 구조 기준 … 구체 세션 일정은 US-6.2에서 ②시작 후 지정(OQ6 해소)", Ref FR2.1/FR6.1. US-3.5: "②시작 후 세션 일정 공유/변경 … 각 세션의 날짜·시간 표시".
- **분모=세션** — US-7.1: (a/S)≥0.80, 정수 비교 a*100≥80*S 고정, S=5/a=4(80% 충족)·S=5/a=3(미충족)·S=6/a=5(83.3% 충족) 예시. US-7.3: "예정 세션을 모두 진행" 후 완료 인정. US-7.4: 출석 세션/전체 예정 세션 대비 현황. US-9.2: "출석율(세션 기준)".
- **상태머신** — "진행중 단계의 순서 [rev2]" 노트 present(②후 US-3.6 응답 + US-6.2 일정 + US-6.3 팝업 출석).
- **MoSCoW** — Must 30 / Won't 1 / 총 31 정합. Epic③ = US-3.1~3.6(6개). 합산 4+4+6+4+1+3+4+2+2 = 30 (검산: 8→14→18→19→22→26→28→30). US-9.3만 Won't.
- **OQ/A** — requirements: OQ5·OQ6 해소, OQ7 present, A5·A6 present; stories Assumptions에 "OQ5·OQ6 해소" 및 OQ7/A5/A6 반영. 계약 #2 목록에 user[사번]/meeting/survey question·answer/post·attachment/session/attendance 포함.
- **센서** — required-sections: stories.md H2 다수(≥2), personas.md·requirements.md 각 ≥2 충족. upstream-coverage: stories.md가 requirements.md(FR/NFR 출처)와 team-practices(횡단 AC·Testing Posture 상속)를 prose에서 참조 → 충족.
- **의존성 무결성** — 모든 deps 참조 스토리 실재(US-2.3→2.1a/3.2/3.6, US-3.5→3.2/6.2, US-3.6→2.1b/6.1, US-6.3→6.2, US-7.1→6.3, US-7.4→6.3/7.2, US-9.2→6.3/7.1). orphan·broken·역방향 참조 없음.

### Suggestions (non-blocking)

- **S1** — US-1.3/requirements FR1.4의 "project.md Decided(히든 IP 안티-중복) reconcile 필요"는 아티팩트 결함이 아니라 하류 정합화 항목으로 정확히 표기됨. delivery-planning/functional-design에서 project.md Decided 갱신을 잊지 않도록 이월 추적만 확인하면 충분.
