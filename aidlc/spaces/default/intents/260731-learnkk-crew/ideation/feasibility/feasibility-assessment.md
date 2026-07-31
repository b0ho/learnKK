# Feasibility Assessment — learnKK (런크크)

<!-- 기술적 실현 가능성·리스크 분석. Architect 리드 + AWS-platform·Compliance 관점 반영. 출처: 사용자 답변(Feasibility Q1~Q5), intent-statement, market-research(competitive-analysis / market-trends / build-vs-buy). -->

## 요약 판정: 실현 가능 (Feasible, 저위험)

learnKK는 표준적인 웹 애플리케이션 패턴으로 로컬에서 완결 구현 가능하며, 신규 기술 리스크나 외부 통합 리스크가 낮다. `build-vs-buy.md`의 "자체 구축·전부 로컬" 결정과 일관되고, `intent-statement.md`의 범위(설계까지)와 `market-trends.md`의 모바일 우선 방향에도 부합한다.

## 기술 스택 (팀 선택)

- **프론트엔드:** React (모바일 웹뷰 우선 UX — `market-trends.md`의 모바일 우선·짧은 상호작용 패턴과 정합)
- **백엔드:** Java Spring
- **데이터 저장:** PostgreSQL (관계형 — 모임·회원·출석·수료 등 관계형 데이터에 적합)
- 전부 로컬 실행, 외부 SaaS/AWS 미사용

## 기술 viability 분석

- 요구 기능(모임 개설·승인·모집·주차 진행·자료실·공지·쪽지·출석·수료 판정)은 `competitive-analysis.md`의 table-stakes와 동일하며, React+Spring+PostgreSQL 조합으로 무리 없이 구현 가능한 CRUD·상태전이·파일 업로드·메시징 수준이다.
- 출석율 기반 수료 판정(멘티 80%)은 단순 집계 로직으로 구현 가능(`intent-statement.md` Success Metrics).
- 파일(자료실) 업로드는 로컬 파일시스템/DB로 처리 가능.

## AWS / 인프라 관점 (aws-platform)

- 이번 범위에서는 **클라우드 인프라 없음** — 전부 로컬. 배포·연계는 향후 별도 워크플로우로 분리(`intent-statement.md` Initial Scope Signal).
- 추후 외부 배포가 필요해지면 그 시점에 인프라 설계를 별도로 진행(현재는 N/A).

## 보안·규제 관점 (compliance)

- 수집 개인정보는 최소(닉네임, 비밀번호, 관심사 해시태그, 한 줄 소개). 비밀번호는 **해시 저장**.
- **신규 제약:** 회원가입에 승인 절차가 없으므로, 중복/악용 계정을 막기 위한 **히든 안티-중복계정 장치(IP 등 신호 활용)** 를 둔다(사용자 요청, Feasibility Q3).
  - 개인정보 관점 주의: IP는 개인정보로 취급될 수 있어, 수집 목적을 중복 방지로 한정하고 최소 보관·비노출(히든) 원칙을 적용한다. 로컬 파일럿 범위에서는 규제 리스크 낮음.
- 특정 규제(PCI/HIPAA 등)는 해당 없음.

## Assumptions & Open Questions

- 구체적 스키마·API·모듈 구조는 application-design / functional-design에서 확정.
- IP 기반 중복 방지의 구체 방식(정확도·오탐 처리)은 nfr-requirements/functional-design에서 상세화.
