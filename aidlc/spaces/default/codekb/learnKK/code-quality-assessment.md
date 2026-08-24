# 코드 품질 평가 — learnKK

## 테스트/검증 현황
- **백엔드**: 서비스(Mockito)·웹(@WebMvcTest)·통합(Testcontainers)·계약(OpenApiContractTest 23케이스 green) 다층 구성. bolt4~7 통합 후 `compileJava`/`compileTestJava` 통과, 계약 테스트 green 확인.
- **프론트엔드**: 컴포넌트별 `*.test.tsx`(Vitest+RTL). 통합 후 `tsc --noEmit` 통과 확인.
- **coverage**: 80% line floor 목표(JaCoCo/Vitest), 도메인 규칙 분기는 시나리오 커버리지로 보강(팀 posture).

## 린트/포맷/CI
- FE: ESLint + Prettier. BE: Spotless(google-java-format) + Checkstyle.
- CI/CD: 이번 프로젝트 범위 밖(로컬·설계 전용, 구현 워크플로우로 이월). 파이프라인/operation 단계는 실행하지 않음(project.md Scope Overrides).

## 강점
- 계약 우선(OpenAPI 단일 소스) + 계약 테스트로 FE/BE interface drift 억제.
- 상태전이가 단일 조건부 UPDATE로 원자화되어 경쟁·불법 전이에 안전.
- 크로스모듈이 read 포트로만 연결되어 순환 없음.

## 기술 부채 / 리스크 (이번 버그픽스 맥락)
- **세션 종료가 시간 파생만** — 멘토가 명시적으로 "완료 처리"할 수단이 없음(#8). 스키마 보강 필요.
- **관리자 화면이 id 조회 방식** — 대기 목록을 못 봄(#2/#3). 상태별 목록 조회 엔드포인트 부재.
- **역전이(되돌리기) 부재**(#5) — 잘못 승인 시 복구 불가.
- **신청 취소 후 재신청 불가**(#12) — CANCELLED 행이 UNIQUE(meeting,mentee)를 점유, apply()가 존재 여부만으로 중복 차단.
- **사전설문 문항이 IN_PROGRESS부터 lock**(#10) — ②시작 후 문항 조정 불가.
- **SurveyBuilder 선택지 입력**에서 매 키 입력마다 `split(',').filter(Boolean)`로 빈 조각 제거 → 쉼표 입력 즉시 소멸(#1).
- 승인 액션에 확인 다이얼로그 부재(#6), 자료실 진입 링크 저강도(#9), 피드백/사전설문 혼재 표시(#11).

## 개선 우선순위
버그픽스 12건은 대부분 국소 변경(FE UI + 소수 BE 엔드포인트/마이그레이션). 계약 테스트·컴파일을 회귀 가드로 사용.
