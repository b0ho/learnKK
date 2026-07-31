# Constraint Register — learnKK (런크크)

<!-- 기술·조직·규제 제약. 출처: 사용자 답변(Feasibility Q1~Q5), intent-statement, build-vs-buy. -->

## 기술 제약 (Technical)

| ID | 제약 | 근거 |
|----|------|------|
| T1 | 프론트엔드는 React, 백엔드는 Java Spring 사용 | 팀 선택 (Q1) |
| T2 | 데이터 저장은 PostgreSQL(관계형) | 팀 선택 (Q2) |
| T3 | 전부 로컬 환경에서 개발·실행, 외부 SaaS/AWS 미사용 | build-vs-buy.md, intent Initial Scope Signal, Q4 |
| T4 | 우선 지원 형태는 모바일 웹뷰(다른 환경 미고려) | intent-statement.md |
| T5 | 이번 워크플로우 범위는 설계까지 — 구현·배포·운영은 별도 워크플로우 | intent-statement.md Initial Scope Signal |

## 조직 제약 (Organizational)

| ID | 제약 | 근거 |
|----|------|------|
| O1 | 3인 개발팀이 각자 배분된 단위(Bolt)를 개별 워크플로우로 구현 | intent / 사용자 지침 |
| O2 | 특별한 마감·일정 제약 없음, 변경 동결·조직적 blocker 없음 | Q5=A |

## 규제·개인정보 제약 (Regulatory / Privacy)

| ID | 제약 | 근거 |
|----|------|------|
| R1 | 수집 개인정보 최소화(닉네임·비밀번호·관심사·한 줄 소개), 비밀번호 해시 저장 | Q3=A, intent |
| R2 | 특정 산업 규제(PCI/HIPAA/SOC2 등) 해당 없음 | Q3 |
| R3 | 히든 안티-중복계정 장치로 IP 등 신호를 활용하되, 목적을 중복 방지로 한정하고 최소 보관·비노출 | Q3(사용자 추가 요청) |

## Assumptions & Open Questions

- 구체 스택 버전·라이브러리 선정은 application-design 범위.
- R3의 IP 활용 방식·보관 정책 세부는 nfr-requirements에서 확정.
