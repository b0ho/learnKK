# 빌드 지침 — 260827-seed-ux-bugfix

## 프론트엔드
- 디렉터리: `frontend/`
- 설치: `npm ci`
- 빌드: `npm run build` (`tsc -b && vite build`)
- 린트: `npm run lint`

> ⚠️ 현재 `npm run build`는 **사전 존재** 타입 오류(`src/routes/AppShell.tsx:42`, TS2345 `TAB_ROOTS.includes(pathname)`)로 실패한다. 이 버그픽스와 무관하며(변경 stash한 베이스라인에서도 동일 재현), 사용자 판단에 따라 별도 수정 대상이다.

## 백엔드
- 디렉터리: `backend/`
- 빌드: `./gradlew build`
- 변경분(`V12__seed_demo.sql`)은 Flyway 마이그레이션 시 적용된다. 데이터 값/주석 변경이므로 Java 컴파일에는 영향 없음.

## 이번 변경 영향 범위
- `backend/.../V12__seed_demo.sql` — 데모 시드 데이터(재빌드 시 초기화). 새 마이그레이션 버전 없음.
- `frontend/.../MeetingListPage.tsx`, `MeetingListPage.test.tsx` — 프론트엔드 전용.
