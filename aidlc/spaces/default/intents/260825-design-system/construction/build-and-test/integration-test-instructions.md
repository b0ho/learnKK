# Integration Test Instructions — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드). 출처: code-generation code-summary.md, intent-statement.md, project.md(신규 모듈 부팅형 검증). 프론트 전용 시각 변경. -->

## 개요

시각 변경은 신규 API·데이터 흐름·백엔드 모듈이 없어 전통적 통합 테스트(백엔드 Testcontainers 등) 대상이 아니다. 대신 **라이브 렌더 스모크**(부팅형 검증)로 토큰·폰트·컴포넌트가 실제 실행 앱에서 정상 렌더됨을 확인한다.

## 라이브 렌더 스모크(부팅형 검증)

- 절차: `cd frontend && npm run dev` → 브라우저로 접속(모바일 폭 390px).
- 확인 항목:
  - 로그인/가입 화면: 그린 브랜드 버튼·링크·라디오 선택색, Pretendard 한글 렌더, 카드/인풋 라운드·보더·그림자 폴리시.
  - 전역 토큰이 CSS 변수로 로드되어 하드코딩 색 없이 일관 적용.
- 결과: ✅ 확인(스크린샷 기록). 콘솔 치명 오류 없음(백엔드 미기동 시 로그인 제출 API 호출은 예상된 네트워크 오류).

## project.md 부팅형 검증 규칙

- "신규 모듈은 최소 1개 부팅형 검증" — 본 변경은 신규 백엔드 모듈/JPA 배선이 없어 백엔드 부팅 검증 대상이 아니며, 프론트 라이브 렌더가 실행 앱 대상 검증을 대체 충족.

## Assumptions & Open Questions

- **[decided]** 백엔드 통합 테스트 무관(무변경) — 미실행.
- **[assumption]** 인증 후 인앱 화면(AppShell 헤더/네비)은 코드 리뷰 + AppShell.test로 검증(백엔드 데이터 없이 전체 로그인 플로우 미실행).
