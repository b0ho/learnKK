# Scalability Requirements — U8 Survey/Feedback (learnKK / 런크크)

<!-- nfr-requirements 산출물. Unit=U8 Survey/Feedback(service). 출처: business-logic-model.md(응답·피드백), business-rules.md, requirements.md(NFR2·NFR4). 파일럿·단일 인스턴스. 경량 CRUD. -->

## 개요

requirements NFR2(동시 수십)·NFR4(단일 인스턴스). 수평 확장 범위 밖. 경량 CRUD라 확장 관심 최소.

## 부하 전망

- 응답: 참여 멘티 × 문항 수(모임당 수십 응답 행). 피드백: 참여 멘티당 1건. 파일럿 규모 완만.
- 조회: 멘토/관리자 조회 저빈도.

## 확장 전략

- 단일 JVM·단일 DB로 충족. 수평 확장 범위 밖(NFR4).
- 데이터 증가 완만 — 아카이빙(=조회 계층 분리, 파기 아님·NFR5 유지) 필요 시 후속.

## Assumptions & Open Questions

- **[assumption]** 인덱스 기반 조회, 완만한 데이터 증가.
- **[open]** 아카이빙 — 범위 밖.
