# Reliability Requirements — U2 Auth & App Shell (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U2(service). 출처: business-logic-model.md(세션·프로필), business-rules.md, requirements.md(NFR4 가용성 단일 인스턴스·범위 밖 HA·NFR5 데이터 보존). 파일럿이라 SLA·HA 없음, 데이터 무결성만 강조. -->

## 개요

requirements NFR4(로컬 단일 인스턴스, HA·다중화·백업 자동화 범위 밖). 신뢰성 요구는 **데이터 무결성**과 **graceful 에러 처리**에 집중하고, 가용성 SLA는 두지 않는다.

## 가용성

- SLA/SLO 없음(파일럿, 단일 인스턴스). 장애 복구·HA는 후속 워크플로우(NFR4).
- 백엔드 재시작 시 서버 세션(DB)이 유지되어 재로그인 최소화 — 서버 세션 채택의 부수 이점.

## 데이터 무결성 (NFR5 영속 보존)

- User·Profile·Session은 PostgreSQL에 영속. 트랜잭션으로 가입(User+Profile 생성) 원자성 보장 — 부분 생성 방지.
- unique 제약(employeeNo·nickname)이 무결성 최종 보증.
- 계정 기록 영속 보존(NFR5) — 파일럿엔 삭제 흐름 없음.

## 장애 처리 (graceful degradation)

- DB 연결 실패 등 인프라 오류는 5xx로 표면화(construction guardrail: silent failure 금지). 사용자에겐 일반 오류 메시지(내부정보 비노출).
- 세션 검증 실패는 401로 명확히 — 앱 셸이 로그인 화면으로 유도(business-logic-model).

## 백업·복구

- 범위 밖(NFR4). 로컬 개발은 docker-compose 볼륨(team-practices Deployment). 운영 백업은 후속.

## Assumptions & Open Questions

- **[assumption]** 가입은 단일 트랜잭션(User+Profile).
- **[open]** 운영 백업·복구·HA — 범위 밖.
- 신뢰성 정식 검증(카오스·복구 테스트)은 이번 범위 밖.
