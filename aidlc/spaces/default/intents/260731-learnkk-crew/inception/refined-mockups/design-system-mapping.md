# Design System Mapping — learnKK (런크크)

<!-- refined-mockups 산출물. React 프론트엔드 전제. Q2=shadcn/ui 우선(불가 시 유사 헤드리스). 출처: mockups.md, interaction-spec.md, team-practices(Code Style), requirements(NFR1/NFR7). 설계 전용 — 실제 설치·구현은 후속 워크플로우. -->

## 라이브러리 선택 [Q2]

- **1순위: shadcn/ui** — Radix UI primitives + Tailwind CSS 기반의 복사형 컴포넌트. 헤드리스 접근성(Radix) + 커스터마이즈 자유도가 높고, 로컬·React 환경에 적합.
- **대체(불가/부적합 시): 유사 헤드리스 라이브러리** — Radix UI 직접 사용 + Tailwind, 또는 Headless UI + Tailwind. 선택 기준: (a) 접근성 primitives 제공, (b) 로컬·번들 부담 낮음, (c) 커스터마이즈 자유. 최종 선택은 구현 워크플로우에서 확정(설계 전용).
- **주의:** shadcn/ui는 npm 패키지 설치형이 아니라 컴포넌트 소스를 프로젝트에 복사하는 방식 — team-practices의 monorepo(`/frontend`)·ESLint/Prettier 규약과 정합. 외부 SaaS 의존 없음(전부 로컬, C2 준수).

## 디자인 토큰(기준값 제안 — Tailwind 매핑)

Q1은 중~고충실도(토큰 강제 아님)이나, shadcn/ui는 CSS 변수 토큰을 쓰므로 최소 토큰 셋을 제안한다(구현서 확정).

| 토큰 | 용도 | 제안 |
|---|---|---|
| color.primary | 주요 액션·링크 | 단일 브랜드 색(대비 AA 확보) |
| color.muted | 보조 텍스트·비활성 | 대비 확보 |
| color.destructive | 반려·오류·삭제 | 색+아이콘/텍스트 병기 |
| color.success | 성공·수료·출석완료 | 색+텍스트 병기 |
| radius | 카드·버튼 모서리 | 일관 반경 |
| spacing | 4px 스케일 | 모바일 터치 타겟 ≥44px |
| font | 한국어 본문 | 시스템 한국어 폰트 우선 |

상태 색(모집중/진행중/모집마감/종료·승인/반려)은 **반드시 텍스트·아이콘 병기**(색상 단독 의존 금지, NFR7/CC-2).

## 컴포넌트 매핑 (mockups·interaction-spec → shadcn/ui)

| 앱 컴포넌트/요소 | shadcn/ui (Radix 기반) | 화면 |
|---|---|---|
| 하단 3탭 네비 | Tabs 또는 커스텀 nav(Radix Tabs 패턴) | 전역 |
| 상태 뱃지(모집중/진행중 등) | Badge (+텍스트 라벨) | 1,2,4,5 |
| 모임 카드 | Card | 1,4,5 |
| 검색 입력 | Input | 1 |
| 해시태그/상태 필터 | Toggle/ToggleGroup | 1 |
| 개설 FAB | Button(floating) | 1 |
| 모임 상세 CTA(신청하기) | Button(+disabled 상태) | 2 |
| 사전설문 폼 | Form + Input/RadioGroup/Textarea | 3,7 |
| 진행률(출석 세션/전체) | Progress (aria-valuenow) | 4,7.4 |
| 내부 탭(공지/자료실/출석/쪽지) | Tabs (tablist/tab/tabpanel) | 4 |
| 세션 일정 관리 | Dialog + Input(date/time) + List | 5 |
| 출석 팝업 | Dialog(aria-modal, 포커스 트랩) | 10 |
| 승인 큐 + 반려 사유 | List + Dialog + Textarea | 6 |
| 자료 게시글 작성 | Form + Textarea + 파일 첨부 Input | 5 |
| 쪽지 목록/스레드 | List + (스레드) | 8 |
| 가입/로그인 폼(사번 포함) | Form + Input | 9 |
| 오류/성공 피드백 | Toast/Alert (aria-live) | 전역 |
| 로딩 상태 | Skeleton | 1,4,6 |

## 반응형 전략 [Q5]

- **기준: 모바일 세로 단일**(대표 폭 360~430px). Tailwind 기본 모바일-퍼스트.
- 태블릿/데스크톱은 이번 설계 범위 밖 — 큰 화면에서는 **콘텐츠 폭 상한(예: max-w-md) 중앙 정렬로 확대 대응**만 하고 레이아웃 재구성은 하지 않음.
- 터치 타겟 최소 44x44px, 하단 탭·FAB은 thumb-reach 고려.

## team-practices 정합

- FE 규약 상속: Prettier + ESLint(+@typescript-eslint), TypeScript 사용, 컴포넌트 PascalCase 파일. (team-practices Code Style)
- API 소비는 단일 API client 계층 경유(컴포넌트에 흩지 않음), JSON camelCase. (team-practices 경계 규약)
- 전부 로컬·외부 SaaS 미사용(C2) — CDN 아이콘/폰트 의존 지양, 로컬 번들.

## Assumptions & Open Questions

- shadcn/ui 최종 채택·컴포넌트 소스 편입·Tailwind 설정은 구현 워크플로우 확정(설계 전용).
- 구체 색/타이포 팔레트(브랜드)는 미정 — 토큰 슬롯만 정의, functional-design/구현에서 값 확정.
