# Intent Statement — 디자인 시스템 적용 (learnKK / 런크크)

<!-- intent-capture 산출물(product 리드 + architect 지원, inline). 출처: 사용자 요청(디자인 투박함 점검·디자인 시스템 적용), 현행 프론트 코드 점검(frontend/src: index.css 토큰·components/ui 9종·tailwind.config.js), refined-mockups design-system-mapping.md(shadcn/ui 확정·브랜드 토큰 이월), project.md(Tech Stack lock·Corrections). 설계 전용 아님 — 구현 스코프(feature). -->

## 배경 (왜)

learnKK 프론트엔드는 refined-mockups에서 **shadcn/ui**로 디자인 시스템이 확정되었고(Q2), 구현 시 shadcn 스택(Radix primitives + Tailwind + CVA + CSS 변수 토큰, `components/ui/` 9종)이 정상적으로 도입되었다. 그러나 사용자 체감상 화면이 "투박"하다. 코드 점검 결과 원인은 shadcn 미적용이 아니라 **디자인 토큰이 기본 스타터값 그대로 방치**된 것이다:

- `index.css`의 `:root` 토큰이 shadcn 기본 스타터 팔레트(거의 흑백 슬레이트) 그대로 — 브랜드 색 부재.
- 커스텀 타이포(폰트 패밀리·타입 스케일) 미지정 — 한글 폰트 미설정.
- 다크모드 토큰(`.dark {}`) 부재(설정만 있고 값 없음).
- elevation/spacing 폴리시 부족, 상태 UI(로딩/빈/오류)가 plain `<p>` — 밋밋함.
- 이는 refined-mockups `design-system-mapping.md`가 "구체 색/타이포 팔레트(브랜드)는 미정 — 토큰 슬롯만 정의, 구현에서 값 확정"으로 이월한 항목이 구현 워크플로우에서 채워지지 않은 결과다.

## 목표 (무엇을)

기존 shadcn/ui 기반 위에 **브랜드 디자인 시스템을 정의하고 전 화면에 적용**해 시각적 완성도를 높인다. 기능 동작과 API 계약은 변경하지 않는다(시각 개선 한정).

- 브랜드 디자인 토큰 정의: 그린 색 팔레트(라이트 완성 + 다크 슬롯 스캐폴드, WCAG AA 대비 목표는 후속 설계/구현에서 명시), 타이포(Pretendard 포함 폰트·타입 스케일), radius/elevation/spacing 폴리시 — `index.css` 토큰 + `tailwind.config.js` 반영.
- 필요한 shadcn/ui 컴포넌트 보강: skeleton, toast(sonner류), dropdown-menu, avatar, tooltip 등(요건 화면에서 실제 쓰이는 것 위주).
- 공통 상태 컴포넌트화: 로딩(skeleton)/빈/오류 상태를 재사용 컴포넌트로(현행 plain `<p>` 대체, CC-3 유지).
- 전 화면(meetings·enrollment·sessions·content·messaging·survey·admin·auth·shell)에 일관 적용, 접근성(CC-2: 색 단독 의존 금지, 텍스트/아이콘 병기) 유지.

## 확정된 방향 (intent-capture-questions 답변, 2026-08-25)

- **Q1 브랜드 메인 색 = 그린 계열** — 학습·성장·친근. 상태색과 구분 용이.
- **Q2 다크모드 = 라이트만** — 다크 토큰 슬롯만 남기고(`.dark {}` 스캐폴드) 값 채움은 이월.
- **Q3 폰트 = Pretendard** — 로컬 self-host(외부 SaaS 의존 없음, C2 준수), 한글 가독성.
- **Q4 레퍼런스 = 없음** — 모던·깔끔·미니멀 기본안.
- **Q5 범위 = 전 화면 일괄** — 토큰·상태 컴포넌트화·컴포넌트 보강 모두 이번에.

## 성공 기준

- 그린 브랜드 팔레트 + Pretendard 타이포가 토큰으로 정의되고 모든 화면에 일관 적용됨(하드코딩 색 없음).
- 라이트 모드 완성, 다크 토큰은 슬롯만 스캐폴드(추후 값 채움 가능).
- 기존 기능·라우팅·API 계약 무변경, 기존 프론트 테스트 무손상(회귀 0).
- 로딩/빈/오류 상태가 공통 컴포넌트로 통일됨.
- 접근성 기존 수준 유지(색 단독 의존 금지, aria/semantic 보존).

## 범위 밖

- 백엔드/API/DB 변경, 기능 추가, 화면 흐름 재설계.
- 파킹된 Bolt 8(U9 Admin/Monitoring) 기능 작업(별도 브랜치 bolt8-admin에서 재개).
- ci-pipeline·operation phase(project.md Scope Override — 구현은 build-and-test에서 종료).

## Assumptions & Open Questions

- **[assumption]** shadcn/ui 유지(교체 아님) — 토큰·컴포넌트 보강만.
- **[assumption]** 모바일 웹뷰 세로 우선(NFR1) 유지.
- **[decided]** 브랜드=그린, 다크=라이트만(슬롯 스캐폴드), 폰트=Pretendard, 레퍼런스 없음, 전 화면 일괄 (intent-capture-questions Q1~Q5).
