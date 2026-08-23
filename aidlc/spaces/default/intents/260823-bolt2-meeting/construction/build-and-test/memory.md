# Build and Test — Observation Diary (Bolt 2 Meeting 완성)

<!-- 오케스트레이터 유지. 손으로 편집하지 않음. build-and-test 스테이지 관측 로그. -->

## Interpretations

- 2026-08-23T10:05:00Z — Bolt 2 build-and-test는 inline 스테이지(quality 리드 + devsecops 지원). Test Strategy=Standard. code-generation에서 test-alongside로 이미 테스트 동반 생성됨 → 이 스테이지는 지시서 문서화 + 실제 빌드/테스트 실행·결과 기록 중심.
- 2026-08-23T10:05:00Z — Bolt 1 build-and-test 산출물 형식을 상속·적응(동일 프로젝트/스택).

## Deviations

- (실행 중 기록)

## Tradeoffs

- 2026-08-23T10:05:00Z — 성능 테스트는 로컬 MVP 범위상 파일럿 가이드만(정식 부하 이월). 보안 테스트는 인증/인가·전이 권한 경계를 단위·슬라이스로 검증(별도 DAST 이월).

## Open questions

- 2026-08-23T10:05:00Z — [env] Testcontainers 통합 테스트: Bolt 1이 문서화한 Rancher Desktop 소켓 `~/.rd/docker.sock`이 현재 환경에 부재. docker-java JNA named-pipe 초기화 실패로 통합 테스트 9건 미실행 — 코드 결함 아님(Bolt 1 AuthIntegrationTest 동일). Docker 접근 가능 환경에서 실행 필요.

- 2026-08-23T10:55:00Z — [verification] Testcontainers 미실행 대체로 라이브 E2E 수행(docker compose Postgres + bootRun + curl). 상태머신 전 과정(T1→T3→T5→T6)·불법전이 409·T4 취소(사유 필수 400 / 사유 포함 200 CANCELLED, reject_reason DB 저장)·인가(403)·listMine 실 DB 통과. 앱 결함 0.
- 2026-08-23T10:55:00Z — [open→learning candidate?] 전역 예외 핸들러가 malformed(비UTF-8) 요청 본문에 500 INTERNAL_ERROR 반환 — 400이 더 적절. Bolt 2 무관 pre-existing 전역 갭. 후속 개선 후보.
- 2026-08-23T10:55:00Z — [note] E2E 관리자 계정: ADMIN 가입 차단 정책상, MENTEE 가입 후 SQL role 승격으로 시드. 실사용 관리자 프로비저닝 절차는 미정(운영 이월).

- 2026-08-23T11:15:00Z — [verification] UI E2E(Playwright, 3역할 탭) 통과: 멘토 개설+운영허브 listMyMeetings, 멘티 상태 경계(승인 전 미노출/후 노출), 관리자 상태 인지 버튼으로 T1→T3→T5→T6 전 전이, 크로스탭 완료 반영.
- 2026-08-23T11:15:00Z — [learning candidate] 백엔드 CORS 허용 origin 기본값이 localhost:5173 단일 → dev 서버가 다른 포트로 뜨면 브라우저 차단. 기본값에 dev 포트 범위(5173~5177) 포함 또는 README 명시 권장. 설정값 이슈(코드 결함 아님).