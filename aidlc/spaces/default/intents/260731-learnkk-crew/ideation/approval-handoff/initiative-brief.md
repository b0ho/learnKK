# Initiative Brief — learnKK (런크크)

<!-- 이데이션 전 단계 산출물을 한 장으로 통합한 이니셔티브 브리프. 인셉션 진행(Go) 판단용. 출처: 모든 ideation 아티팩트 + approval-handoff-questions. -->

한 줄 요약: 러닝 크루(learning crew)의 개설 → 승인 → 모집 → 주차별 진행 → 수료 → 피드백 전 과정을 하나의 모바일 웹뷰 플랫폼으로 통합하는 설계 이니셔티브. 이번 워크플로우는 **설계까지**(이데이션 + 인셉션 상세 설계) 수행하고, 구현·배포는 팀 3인의 개별 워크플로우로 분리한다. **판정: Go (인셉션 진행 권고).**

## 1. Intent & Problem (문제와 의도)

`intent-statement.md`에 따르면, 러닝 크루 운영에 필요한 모집·자료·공지·출석·수료 판정이 여러 도구(현재 wiki)에 흩어져 있어 체계적 진행과 관리자 모니터링이 어렵다. learnKK는 이 흩어진 운영을 하나의 흐름으로 묶는 것이 목표다. `stakeholder-map.md`가 정리한 세 역할 — 멘토(개설·운영), 멘티(신청·학습), 시스템 관리자(감독·수료 판정) — 이 하나의 플랫폼을 공유하며, 시스템 관리자가 방향·범위와 운영 규칙 집행을 모두 담당하는 의사결정권자다.

## 2. Market Validation (시장 검증 요약)

`competitive-analysis.md`는 "온라인 클래스 / 코호트(기수제) 러닝"(클래스101·인프런 결)을 주 벤치마크로 삼았다. 단일 카테고리 경쟁자들은 부분만 커버한다 — 온라인 클래스는 진행·콘텐츠에 강하나 커뮤니티성이 약하고, 챌린지 앱은 출석 동기부여에 강하나 커리큘럼이 얕으며, 모임 매칭은 개설·모집은 쉽지만 학습 진행·수료 관리가 없다. learnKK의 1차 차별점은 **모바일 웹뷰 우선의 가볍고 접근성 높은 UX**(현행 wiki의 낮은 접근성 개선)이며, 전 과정 통합·관리자 감독·출석율 기반 수료 판정이 구조적 차별점으로 따라온다.

## 3. Feasibility & Risk Highlights (실현 가능성·리스크)

`feasibility-assessment.md`의 판정은 **실현 가능(저위험)** 이다. React + Java Spring + PostgreSQL 조합으로 로컬에서 완결 구현 가능한 CRUD·상태전이·파일 업로드·메시징 수준이며, 신규 기술·외부 통합 리스크가 낮다. `constraint-register.md`가 고정한 제약: 프론트 React(T1)·저장 PostgreSQL(T2)·전부 로컬 외부 SaaS 미사용(T3)·모바일 웹뷰 우선(T4)·이번은 설계까지(T5). 개인정보는 최소 수집·비밀번호 해시 저장(R1), 특정 규제 해당 없음(R2), 승인 없는 가입이므로 히든 안티-중복계정 장치(IP 등 신호 활용, 목적 한정·최소보관·비노출, R3). RAID의 리스크·완화는 사용자 확인(Q2=A)으로 그대로 수용됐다.

## 4. Scope Boundary (범위 경계)

`scope-document.md`가 이번 설계 대상을 확정했다. In-scope(전부 Must): 회원가입·프로필, 모임 목록 탐색, 모임 개설, 관리자 승인, 모집·신청+멘티 설문, 자료실·공지, 쪽지(멘토↔멘티·관리자↔멘토/멘티), 출석, 출석율 80% 기반 수료 판정·멘토 완료 인정, 과정 설문·피드백 확인, 관리자 모니터링. Out-of-scope: 실제 구현/코드, 배포·CI·운영 인프라, 게이미피케이션, 결제, 외부 연동·SSO, 리포트 상세화, 프로필 고도화. 시퀀싱은 의존성 우선(dependency-first). `intent-backlog.md`는 이를 11개 Must proto-Unit(회원·인증·프로필 → 모임 개설 → 승인 → 모집·신청 → 목록 → 자료실·공지 → 쪽지 → 출석 → 수료 판정 → 설문·피드백 → 관리자 모니터링)으로 우선순위화했으며, 정식 Unit은 units-generation에서 확정된다.

## 5. Concept Visuals (컨셉 화면)

`wireframes.md`는 모바일 웹뷰(세로) 기준 저충실도 와이어프레임 9종을 정의했다 — 하단 3탭(모임 / 내 러닝(역할 적응형) / 내정보) + 쪽지 전역 헤더 아이콘, 카드형 모임 목록(상태 뱃지·해시태그 필터·기간·정원), 모임 상세·신청 설문·멘티 참여 화면·멘토 관리·관리자 모니터링(승인 큐 4종)·모임 개설·쪽지함·로그인/내정보. 관리자 승인 4지점(① 개설 ② 시작 ③ 멘토 완료 ④ 멘티 수료)과 멘티 수료 자동 판정(출석율 80%)+관리자 승인 구조가 화면·플로우에 일관 반영됐고, reviewer 판정은 READY다.

## 6. Team Plan (팀 계획)

`team-assessment.md`에 따르면 개발자 3인 모두 풀스택(React+Spring+PostgreSQL) 수준으로 스킬 갭이 없다. 분배는 기능 수직 슬라이스(각자 proto-Unit 묶음을 프론트~백엔드 통째 담당), 협업은 독립 병렬(각자 Bolt를 개별 구현 워크플로우로 진행, 단위 간 인터페이스만 사전 합의). 핵심 리스크는 단위 간 인터페이스 불일치이며, 공통 기반(인증·모임 도메인·DB 스키마)을 계약으로 먼저 고정하는 것이 전제 — 구체 순서는 delivery-planning에서 확정한다.

## 7. Go / No-Go Recommendation (진행 권고)

**권고: Go — 인셉션 진행.** 세 역할·핵심 여정·성공 기준(멘토=활동 전부 완료, 멘티=출석율 80% 수료)이 명확하고, 스택·제약·팀 구성이 저위험으로 정렬돼 있다. 사용자 확인 결과(approval-handoff-questions Q1=A Go, Q2=A RAID 수용, Q3=A 특별 전달사항 없음)도 진행을 지지한다. 다음 단계는 인셉션(practices-discovery → requirements-analysis → user-stories → refined-mockups → application-design → units-generation → delivery-planning)이다.

## 8. Open Items Carried Forward (이월 항목)

- proto-Unit 경계와 3인 배분 매핑은 units-generation·delivery-planning에서 확정.
- IP 기반 중복 방지의 구체 방식·보관 정책은 nfr-requirements/functional-design에서 상세화.
- 상태 어휘 통일·승인 큐 반려 UI·과정 설문 전용 화면은 refined-mockups에서 보강(wireframes 비차단 findings).

## Assumptions & Open Questions

None — 미해결 이월 항목은 §8에 명시했다.
