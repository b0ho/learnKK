# Unit Test Instructions — Bolt 8 Admin/Monitoring (learnKK)

<!-- build-and-test 산출물(quality 리드). Standard. test-alongside로 동반 생성됨. -->

## 프레임워크·설정
- 백엔드: JUnit 5 + Mockito(@ExtendWith MockitoExtension) + `@WebMvcTest`(MockMvc, @MockBean AuthService). 프론트: Vitest + RTL.

## 실행
- 백엔드: `cd backend && ./gradlew test --tests "com.learnkk.admin.*"`. 프론트: `cd frontend && npm run test -- --run`.

## 커버리지 대상 (Bolt 8)
| 컴포넌트 | 초점 |
|---|---|
| AdminMonitoringServiceTest | 출석율 집계(0.75 케이스)·수료 확정/후보 카운트·종료 세션 판정, 분모 0→0.0, status 필터 위임, 비관리자 403 |
| AdminMonitoringControllerTest(@WebMvcTest) | 관리자 200(행 직렬화), status 파싱(소문자 허용), 잘못된 status 400, 403 MONITORING_FORBIDDEN, 미인증 401 |
| FE AdminMonitoringPage | 카드 렌더(within 스코프 — 상태 라벨이 필터에도 존재), 필터→status 쿼리 재조회, 빈 목록, 에러 노출 |
| FE api/admin listMonitoring | 라우트·쿼리 직렬화(기본 status 미포함) |

## 목표
- 신규 코드 ≥80% line floor. 집계식은 값 검증(문자열 스냅샷 금지), 인가 경계는 역할별 명시 케이스.
