# Scalability Requirements — U3 Meeting (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U3 Meeting(service). 출처: business-logic-model.md(목록·전이), business-rules.md, requirements.md(NFR2 규모·NFR4 단일 인스턴스). 파일럿·로컬 단일 인스턴스. -->

## 개요

requirements NFR2(모임 수십 개·동시 수십 명)·NFR4(단일 인스턴스). 수평 확장 범위 밖.

## 부하 전망

- 모임: 진행 중 수십 개(파일럿). meeting 테이블·status 인덱스로 충분.
- 모집 목록 조회: 멘티 다수 동시 조회 가능 — 읽기 위주, 인덱스+페이지네이션으로 선형.

## 동시성

- 상태 전이 경합(이중 승인): 조건부 UPDATE(`WHERE status=<expected>`)로 직렬화 → 하나만 성공, 409(BR-U3-1). DB 수준 원자성으로 애플리케이션 락 불요.
- 개설·사전설문 템플릿 편집은 소유 멘토 단독이라 경합 낮음.

## 확장 전략

- 단일 JVM·단일 DB로 목표 충족. 수평 확장은 범위 밖(NFR4).
- 데이터 증가: meeting/survey_template 완만 — 종료 모임 아카이빙은 [open] 후속.

## Assumptions & Open Questions

- **[assumption]** 목표 규모 파일럿. status 인덱스.
- **[open]** 종료 모임 아카이빙·목록 대량화 대응 — 범위 밖.
- 용량 계획·오토스케일은 범위 밖.
