# Scope Document — learnKK (런크크)

<!-- 범위 경계(In/Out)와 우선순위. 출처: 사용자 답변(Scope Q1~Q4), intent-statement, feasibility-assessment, constraint-register. -->

## 이번 워크플로우 경계

- 이 워크플로우는 **설계까지**(이데이션 + 인셉션 상세 설계) 수행한다. 실제 구현·배포·운영은 팀원 3인의 개별 구현 워크플로우로 분리한다. (`intent-statement.md` Initial Scope Signal)
- 스택·제약은 `feasibility-assessment.md`·`constraint-register.md`에서 확정: React + Java Spring + PostgreSQL, 전부 로컬, 최소 보안 + 히든 IP 중복방지.

## In Scope (이번 설계 대상 — 전부 Must)

첫 버전의 핵심 여정 전체가 Must-have다 (Scope Q1=A~E, `intent-statement.md`):

- 회원가입·프로필 — 닉네임+비밀번호 가입(승인 없음), 관심사 해시태그·한 줄 소개
- 모임 목록 탐색
- 멘토 모임 개설(주제·학습기간·모집기간·정원·진행방식·학습자료·공지)
- 관리자 승인 → 정식 모임 전환
- 모집·신청 + 멘티 설문
- 주차별 진행: 자료실 업로드, 공지사항
- 쪽지(메시징): 멘토 ↔ 멘티, 관리자 ↔ 멘토/멘티 (Must — Scope Q2에서 미루지 않음)
- 출석: 모임 시작/종료, 멘티 출석 체크
- 수료 판정: 출석율 80% 기준(멘티), 멘토 완료 인정
- 과정 설문 + 멘토 피드백 확인
- 관리자 모니터링

## Out of Scope (명시적 제외)

(Scope Q3=A~E, + Q2에서 Could로 검토됐던 항목도 이번 범위에서 제외 확정)

- 실제 구현/코드 — 이번은 설계까지
- 배포·CI·운영·모니터링 인프라 — 향후 별도 워크플로우
- 게이미피케이션(스트릭·뱃지) — `market-trends.md`에서 제외
- 결제·정산 등 금전 기능
- 외부 시스템 연동·SSO 등 (`constraint-register.md` T3: 외부 SaaS 미사용)
- 과정/멘토 피드백 리포트 상세화 — 불필요(사용자 결정)
- 프로필 고도화(관심사 기반 추천 등) — 불필요(사용자 결정)

## 시퀀싱 방침

- **의존성 우선(dependency-first)** (Scope Q4=A): 회원·모임 기반 → 모집·신청 → 진행(자료·공지·쪽지)·출석 → 수료·설문·피드백. 관리자 모니터링은 전 과정을 가로지르는 cross-cutting.
- 이 순서는 units-generation·delivery-planning에서 3인 배분의 기준이 된다.

## Assumptions & Open Questions

- 세부 우선순위(Could 항목의 반영 깊이)는 requirements-analysis에서 조정 가능.
