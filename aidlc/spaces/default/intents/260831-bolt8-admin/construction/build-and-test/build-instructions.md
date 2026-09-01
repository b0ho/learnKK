# Build Instructions — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(quality 리드 + devsecops 지원). Bolt 7 상속·적응. -->

## 사전 요구사항
- Java 21, Node 20+/npm, Docker(PostgreSQL·Testcontainers). 시크릿 `.env`(커밋 금지).

## 환경 설정
1. `cp .env.example .env`. 2. `docker compose up -d db`(PostgreSQL 16). 3. 백엔드 `SPRING_PROFILES_ACTIVE=local`. **Bolt 8은 마이그레이션 추가 없음** — Flyway V1~V10 그대로(관리자 시드=V10).

## 빌드 명령
- 백엔드(`/backend`): `./gradlew build`. 신규 모듈만: `./gradlew test --tests "com.learnkk.admin.*"`.
- 프론트(`/frontend`): `npm install` → `npm run build`. 개발 `npm run dev`.

## 빌드 검증
- 백엔드: BUILD SUCCESSFUL, 전체 테스트 통과. 프론트: `tsc -b` 0 에러, vite build, eslint 0.
- 수동 확인: 관리자 로그인(V10 시드) → 관리 탭(승인 큐) → '운영 현황' 버튼 → `/admin/monitoring` 카드·필터 동작.

## 트러블슈팅
- 통합 테스트 Docker 미가용: 단위/슬라이스는 Docker 없이 실행(`com.learnkk.admin.*`는 전부 비통합).
- 구버전 record 생성자 컴파일 에러가 다시 보이면: 파킹 트리(bolt8-admin WIP) 잔재 — 본 브랜치의 현행화된 테스트 파일인지 확인.
