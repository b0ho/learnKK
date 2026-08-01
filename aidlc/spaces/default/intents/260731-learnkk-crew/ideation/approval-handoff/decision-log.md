# Decision Log — Ideation (learnKK / 런크크)

<!-- 이데이션 전 단계에서 확정된 주요 결정 기록. 출처: 각 stage 아티팩트 + questions. 인셉션의 되묻기 방지용 참조. -->

이데이션 6개 스테이지에서 확정된 결정을 한 곳에 모은다. 각 결정은 인셉션에서 다시 묻지 않는다(변경은 정식 change request로만).

## 결정 목록

| # | 결정 | 근거 스테이지 / 출처 |
|---|------|---------------------|
| D1 | 이번 워크플로우는 **설계까지**(이데이션 + 인셉션 상세 설계) 수행하고, 구현·배포·운영은 팀 3인의 개별 워크플로우로 분리한다 | intent-capture / scope-definition (`intent-statement.md`, `scope-document.md`) |
| D2 | 세 역할(멘토·멘티·시스템 관리자)이 하나의 모바일 웹뷰 플랫폼을 공유하고, 시스템 관리자가 방향·범위·운영 규칙 집행의 의사결정권자다 | intent-capture (`stakeholder-map.md`) |
| D3 | 성공 기준: 멘토 = 계획된 주차별 활동 전부 완료, 멘티 수료 = 출석율 80% 이상 | intent-capture (`intent-statement.md` Success Metrics) |
| D4 | 주 벤치마크는 온라인 클래스/코호트 러닝, 1차 차별점은 모바일 웹뷰 우선 접근성 UX | market-research (`competitive-analysis.md`) |
| D5 | 게이미피케이션·결제·외부 연동/SSO·리포트 상세화·프로필 고도화는 이번 범위에서 제외 | market-research / scope-definition (`market-trends.md`, `scope-document.md`) |
| D6 | 기술 스택: React + Java Spring + PostgreSQL, 전부 로컬, 외부 SaaS/AWS 미사용 | feasibility (`feasibility-assessment.md`, `constraint-register.md` T1~T4) |
| D7 | 개인정보 최소 수집 + 비밀번호 해시 저장, 특정 규제 해당 없음 | feasibility (`constraint-register.md` R1·R2) |
| D8 | 승인 없는 가입이므로 히든 안티-중복계정 장치(IP 등 신호 활용, 목적 한정·최소보관·비노출)를 둔다 | feasibility (`constraint-register.md` R3, project.md Decided) |
| D9 | In-scope 11개 핵심 여정 전부 Must, 시퀀싱은 의존성 우선(dependency-first) | scope-definition (`scope-document.md`, `intent-backlog.md`) |
| D10 | 관리자 승인 4지점: ① 모임 개설 ② 모임 시작 ③ 멘토 정상 완료 ④ 멘티 수료 | rough-mockups (project.md Decided u1) |
| D11 | 멘티 수료 = 출석율 80% 시스템 자동 판정 + 관리자 승인④. 멘토는 멘티 수료를 인정하지 않고, 모임 완료 인정 신청(승인③)과 멘티 피드백 확인만 담당 | rough-mockups (project.md Decided u2) |
| D12 | 멘티 피드백(과정 설문)은 멘토가 확인하고 시스템 관리자도 열람 가능 | rough-mockups (project.md Decided u3) |
| D13 | 사전 신청 설문 문항은 멘토가 모임 개설 시 자유롭게 구성하고, 멘티는 신청 시 응답만 한다 | rough-mockups (project.md Decided u4, `wireframes.md`) |
| D14 | 팀 3인 전원 풀스택, 기능 수직 슬라이스 배분 + 독립 병렬 협업, 공통 기반은 인터페이스 계약으로 선고정 | team-formation (`team-assessment.md`) |
| D15 | 인셉션 진행(Go) 확정 — 특별 전달 제약 없음, 산출물·project.md 규칙으로 충분 | approval-handoff (`approval-handoff-questions.md` Q1·Q2·Q3 = A) |

## 이월(Deferred) 결정

- proto-Unit 경계·3인 배분 매핑 → units-generation / delivery-planning
- IP 기반 중복 방지 구체 방식·보관 정책 → nfr-requirements / functional-design
- 상태 어휘 통일·승인 큐 반려 UI·과정 설문 전용 화면 → refined-mockups

## Assumptions & Open Questions

None.
