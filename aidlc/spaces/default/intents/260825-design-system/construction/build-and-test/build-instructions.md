# Build Instructions — 디자인 시스템 적용 (learnKK / 런크크)

<!-- build-and-test 산출물(quality 리드). 출처: code-generation code-summary.md, intent-statement.md, 기존 frontend 빌드 규약(bolt1). 프론트 전용 변경. -->

## 개요

디자인 시스템 변경의 프론트엔드 빌드 절차. 기존 Vite+React+TS 빌드 파이프라인을 그대로 사용하며 신규 빌드 단계는 없다(폰트 self-host는 Vite 자산 번들에 포함).

## 사전 요건

- Node.js + npm. 저장소 `frontend/`에서 실행.
- 신규 의존성 설치 필요: `pretendard`, `sonner`, `@radix-ui/react-{tooltip,avatar,dropdown-menu}` (package.json 반영됨).

## 빌드 단계

1. `cd frontend`
2. `npm install` — 신규 의존성 설치(락파일 반영). 외부 CDN 없음(폰트 self-host).
3. `npm run build` — `tsc -b`(타입체크) + `vite build`(번들). Pretendard variable woff2가 `dist/assets/`에 로컬 번들.
4. 산출물: `dist/`(정적 SPA). `VITE_API_BASE`로 백엔드 주소 주입(기본 `http://localhost:8080`).

## 검증

- 빌드 green(§build-test-results). 타입 에러 0(사전 존재 3건 정리 포함).

## Assumptions & Open Questions

- **[decided]** 백엔드 빌드는 이번 변경 대상 아님(무변경).
- **[assumption]** 로컬 환경 전제(C2) — 외부 폰트 CDN·SaaS 미사용.
