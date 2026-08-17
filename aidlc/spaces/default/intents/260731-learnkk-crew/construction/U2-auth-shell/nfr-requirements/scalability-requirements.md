# Scalability Requirements — U2 Auth & App Shell (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U2(service). 출처: business-logic-model.md(세션검증 전처리), business-rules.md(세션), requirements.md(NFR2 규모·NFR4 단일 인스턴스). 파일럿·로컬 단일 인스턴스라 확장은 설계 목표 최소. -->

## 개요

requirements NFR2(동시 수십 명·모임 수십 개)·NFR4(로컬 단일 인스턴스). 수평 확장·오토스케일은 범위 밖. U2는 세션·사용자 규모의 상한만 확인.

## 부하 전망

- 사용자: 수백 명 규모(파일럿). User 테이블·인덱스(nickname/employeeNo unique)로 충분.
- 세션: 동시 수십 활성 세션. 세션 검증 요청/초는 전 API 호출과 비례 — 인덱스 조회로 선형 처리.

## 확장 전략 (해당 시)

- 단일 JVM·단일 DB로 목표 규모 충족. 수평 확장 필요 시(범위 밖) 서버 세션이 걸림돌이 될 수 있어 그 시점에 JWT/외부 세션 스토어 재검토 — 현재 [open], 후속.
- 데이터 증가: 세션 테이블만 지속 증가 → lazy 만료 정리(performance-requirements).

## 동시성

- 가입 동시 경합(같은 사번/닉네임): DB unique 제약이 직렬화 보증 → 409(BR-U2-1b/2). 애플리케이션 락 불요.

## Assumptions & Open Questions

- **[assumption]** 목표 규모는 파일럿(수백 사용자·수십 동시). 초과 시 재설계.
- **[open]** 수평 확장 시 세션 스토어 전략(현재 서버 세션) — 범위 밖.
- 용량 계획·오토스케일은 이번 범위 밖(NFR4).
