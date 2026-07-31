# RAID Log — learnKK (런크크)

<!-- Risks / Assumptions / Issues / Dependencies. 출처: Feasibility 답변, intent-statement, competitive-analysis, market-trends, build-vs-buy. -->

## Risks (리스크)

| ID | 리스크 | 영향 | 완화 |
|----|--------|------|------|
| RK1 | 승인 없는 닉네임+비밀번호 가입 → 중복/악용 계정 | 데이터 품질·운영 부담 | 히든 IP 기반 중복 방지 장치(Q3); 관리자 모니터링 |
| RK2 | 기간제 학습 중도 이탈(완주율 저하) | 수료율·서비스 성과 | 출석 현황 가시화·공지·쪽지로 지속 유도(market-trends.md) |
| RK3 | 3인 병렬 구현 시 단위 간 인터페이스 불일치 | 통합 지연 | units-generation·delivery-planning에서 경계·의존성 명확화 |
| RK4 | 설계-구현 워크플로우 분리로 인한 컨텍스트 손실 | 구현 품질 | 상세 설계 산출물(functional-design)로 자립 가능 수준 명세 |

## Assumptions (가정)

- A1: 전부 로컬에서 완결 개발 가능하며 외부 통합은 없다 (Q4=A).
- A2: 일정·조직적 blocker가 없어 설계에 집중할 수 있다 (Q5=A).
- A3: 벤치마크(코호트 러닝)의 진행·자료·수료 흐름을 참고 기준으로 삼는다 (competitive-analysis.md).
- A4: 비밀번호 해시 등 최소 보안으로 파일럿 범위 충족 (Q3=A).

## Issues (현재 이슈)

- I1: state의 Project 필드가 플레이스홀더로 남아 있음(원문 설명 미저장) — 진행에는 영향 없으나 추후 정리 대상.

## Dependencies (의존성)

- D1: 이번 설계 산출물 → 팀원별 구현 워크플로우의 입력.
- D2: units-generation·delivery-planning → 3인 작업 분배(team-formation)와 연결.
- D3: intent-statement / market-research 결정 → application-design·functional-design의 상위 근거.

## Assumptions & Open Questions

- 위 리스크·가정은 이후 nfr-requirements / functional-design에서 구체 완화책·설계로 이어진다.
