# Tech Stack Decisions — U9 Admin/Monitoring (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U9 Admin/Monitoring(service, read 계층). 출처: business-logic-model.md(read 조합), business-rules.md(BR-U9-5), requirements.md(C1·FR9.1·FR9.2 TBD), U1 tech-stack 상속. U9는 read 조합 기술 선택(소유 저장 없음). -->

## 개요

U1 스택·계약 도구 상속. U9는 소유 데이터가 없어 신규 저장 결정이 없고, **read 조합 방식**만 확정.

## U9 기술 선택

### TD-U9-1. read 조합 — in-process Service read

- **결정:** 승인 큐·모니터링은 U3/U4/U5의 **Service 인터페이스 read**를 in-process로 조합(services.md, 모듈러 모놀리스). 직접 테이블 접근·조인 금지(모듈 소유, BR-U9-5).
- **근거:** 각 Unit이 데이터 권위 유지. 단일 프로세스라 in-process 호출로 조합 비용 낮음.

### TD-U9-2. N+1 회피 — 배치 조합

- **결정:** 모임 목록 조회 후 U4/U5 집계를 배치(id 목록 기반)로 결합 — 모임당 개별 왕복 지양.
- **근거:** 파일럿 규모라도 N+1 방지로 <2초 목표 안정.

### TD-U9-3. 저장·캐시 — 없음

- **결정:** U9 소유 테이블·캐시 없음. 매 조회 시 소유 Unit read(stale 없음). 집계 지표 저장은 범위 밖(FR9.2 TBD).

## 범위 밖

- 집계 지표 사전계산·대시보드 캐시(FR9.2 TBD·US-9.3 Won't), BI 도구. CI/CD·운영(C3).

## Assumptions & Open Questions

- **[decided]** in-process Service read 조합, 배치(N+1 회피), 소유 저장·캐시 없음.
- **[open]** U3 listByStatus·U5 listByCompletion/allScheduledSessionsEnded·U4 count read 포트 시그니처(U3/U4/U5 계약). 집계 지표(FR9.2)는 범위 밖.
