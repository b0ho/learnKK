# Application Design — Observation Diary

<!-- Auto-created at stage start. Maintained by the orchestrator. Not hand-edited by the user. -->

## Interpretations

- 2026-08-17T14:30:00Z — 전부 로컬(외부 SaaS/AWS 미사용, C2)이라 aws-platform 관점은 클라우드 매핑 대신 "로컬 단일 인스턴스" 전제로 최소 적용. team-practices가 3계층(Controller/Service/Repository)·DTO 경계·monorepo·3계약(#1 OpenAPI/#2 DB/#3 도메인 타입)을 이미 고정 → application-design은 그 위에서 컴포넌트/도메인 분해·서비스·의존성·ADR을 구체화.

## Deviations

## Tradeoffs

- 2026-08-17T15:00:00Z — 모듈러 모놀리스 + C0 shared kernel(상태/수료 enum)을 계약 #3의 물리 소유로 배치. 상태 전이 집행은 C2 단일 소유로 몰아 병렬 interface 불일치를 구조적으로 차단(ADR-006).

## Open questions

- 2026-08-17T15:05:00Z — 리뷰어 B1: ③완료 전제조건(전 세션 종료)이 C4 소유 상태라 C2→C4 read가 불가피한데 초안이 "C2→C4 없음"으로 단언해 자기모순. read 상호참조(C2↔C3, C2↔C4)를 Y/R 표기로 구분하고 read port/오케스트레이션으로 해소(ADR-007 확장). 교훈: 전제조건 집행은 그 상태를 소유한 모듈에 대한 read 의존을 반드시 명시할 것.
- 2026-08-17T15:06:00Z — C4→C3 라벨(R이 정확), C8→C7 근거 명시는 functional-design 이월(리뷰어 비차단).
