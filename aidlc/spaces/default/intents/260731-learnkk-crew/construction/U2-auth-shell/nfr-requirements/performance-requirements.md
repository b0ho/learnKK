# Performance Requirements — U2 Auth & App Shell (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U2(service). 출처: business-logic-model.md(W1~W4·세션검증 전처리·FE 셸), business-rules.md(BR-U2-3 세션·BR-U2-1 bcrypt), requirements.md(NFR2 규모·NFR3 성능 1~2초·NFR1 모바일 웹뷰). U1 성능 규약 없음(계약 Unit) — 공통 baseline은 services.md. -->

## 개요

파일럿 규모(NFR2: 동시 수십 명)·로컬 단일 인스턴스 전제. 목표는 엄격 SLA가 아닌 체감 응답 1~2초(NFR3). U2는 **모든 보호 요청의 전처리(validateSession)**를 소유하므로, 세션 검증 경로의 저지연이 전 시스템 성능에 영향.

## 응답 시간 목표 (가이드, NFR3)

| 작업 | 목표 | 근거 |
|------|------|------|
| 로그인(bcrypt 검증) | < 1초 | bcrypt 비용 인자 튜닝 대상 |
| 세션 검증(요청당) | < 50ms | 매 보호 요청 전처리 — 인덱스 조회 |
| 가입 | < 1초 | bcrypt 해시 + 2 unique 검증 |
| 프로필 조회/수정 | < 500ms | 단순 단건 |

## 핵심 성능 고려

- **bcrypt 비용 인자:** 보안(느릴수록 안전)과 로그인 지연의 트레이드오프. 파일럿 기본 cost=10~12 [assumption], 로그인 < 1초 유지 범위에서 선택. CPU 바운드라 동시 로그인 폭주 시 병목 가능하나 파일럿 규모에선 무시.
- **세션 검증 경로:** `session.token`에 unique 인덱스 → O(log n) 조회. 매 요청 전처리이므로 인덱스 필수. 서버 세션(DB) 조회가 요청당 1회 추가되나 단일 인스턴스·소규모라 무리 없음(services.md).
- **FE 앱 셸:** 모바일 웹뷰 초기 로드 최적화(번들 분할·shadcn tree-shaking). 단일 API client 재사용으로 중복 요청 회피.

## 부하·자원

- 동시 수십 세션 검증/초 수준 — 단일 JVM으로 충분. 세션 테이블 증가는 만료 세션 주기 정리 [assumption](배치 없음 → 조회 시 만료 무시 + 선택적 정리).

## Assumptions & Open Questions

- **[assumption]** bcrypt cost 10~12, 세션 만료 정리 방식(lazy).
- **[open]** 엄격 성능 검증은 performance-validation(범위 밖 — 이번 스코프 SKIP). 여기 수치는 설계 가이드.
- 상세 벤치마크·부하 테스트는 후속 구현 워크플로우.
