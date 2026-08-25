# Performance Test Instructions — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드). 출처: code-generation code-summary.md, intent-statement.md(NFR1 모바일 웹뷰), tailwind/vite 번들 산출. 프론트 전용 변경. -->

## 개요

모바일 웹뷰 우선(NFR1)에서 디자인 시스템 변경이 초기 로드 성능에 주는 영향을 확인한다. 핵심 변수는 폰트 자산 크기다.

## 측정·기준

- **폰트 번들:** Pretendard **variable 단일 woff2 ~2.06MB** 채택(static 9종 ~13MB 대비 대폭 축소). 브라우저는 필요 시 1개 파일만 로드.
- **JS/CSS 번들:** `index.js` 370.32KB(gzip 115.64KB), `index.css` 23.93KB(gzip 5.31KB) — 토큰/컴포넌트 추가에도 CSS gzip 5KB대 유지.
- **로드 최적화:** 폰트 self-host(외부 CDN 왕복 없음), Vite 코드 분할·트리셰이킹 유지.

## 권고(비블로킹)

- 추가 축소가 필요하면 Pretendard **dynamic-subset**(한글 사용 글리프만 조각 로드)으로 전환 가능 — 현 파일럿 규모에선 단일 variable로 충분.

## Assumptions & Open Questions

- **[decided]** variable 폰트로 번들 축소 완료(모바일 우선).
- **[assumption]** 파일럿 규모라 엄격 성능 SLA 없음(체감 로드 개선이 목표).
