# External Dependency Map — learnKK (런크크)

<!-- delivery-planning 산출물. 게이트성 외부 의존(외부 API·데이터 창·승인 리드타임·외부팀 핸드오프)을 Bolt에 매핑. 출처: bolt-plan, requirements(C2 전부 로컬), team-practices. -->

## 결론: 외부 의존성 없음

이 프로젝트는 **전부 로컬·팀 자체 완결**이다(requirements C2: 외부 SaaS·클라우드·SSO·외부 연동 미사용). 따라서 Bolt 진행을 막는 **외부 게이트 의존이 없다**(Q5=A).

| 유형 | 항목 | 대응 |
|------|------|------|
| 외부 API | 없음 | — |
| 데이터 가용 창 | 없음 | — |
| 승인 리드타임(외부) | 없음 | (앱 내 관리자 승인은 도메인 기능이지 외부 의존 아님) |
| 외부팀 핸드오프 | 없음 | 3인 자체 완결 |

## 내부(팀 내) 선행 — 참고

외부는 아니나, 팀 내부의 하드 선행(구현 착수 시)은:
- **공유 계약 선고정(#1/#2/#3)** — Bolt 1에서 확정되어야 Bolt 2~8 병렬 착수 가능. (external 아님, team-practices 내부 계약)
- **로컬 실행 환경** — docker-compose(PostgreSQL 등, team-practices Deployment). 로컬 도구라 외부 의존 아님.

## Assumptions & Open Questions

- 향후 외부 연동(SSO·알림 등)을 도입하면 이 맵을 갱신 — 현재 범위(전부 로컬)에선 빈 맵.
