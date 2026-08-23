# Security Test Instructions — Bolt 5 Messaging

<!-- devsecops 지원. 상류: bolt5-messaging code-generation-plan.md · code-summary.md · learnkk-crew U7 nfr-requirements/security-requirements.md. -->

## 위협·검증 항목 (STRIDE 관점)

- **인가 우회(Elevation/Info Disclosure)**: 권한 경계가 서버 send/read에서 강제되는가.
  - 멘토→무관 멘티 403, 멘티→무관 멘토 403(unit + web).
  - 비참여자의 스레드 열람 403(`getThread` 참여자 게이트).
  - 무인증 접근 401(`SessionAuthInterceptor` `/api/messages/**` 보호).
- **입력 검증(Tampering)**: 빈 본문 400, 자기발신 400, 미존재 상대 404, 정렬 필드 화이트리스트(`createdAt`)만 허용.
- **정보 노출**: 스레드 유일성(정규화 쌍)·상대 식별은 참여자에게만. 에러 바디는 균일 `{code,message,details}`(내부 노출 없음).

## 실행 방법

- `cd backend && ./gradlew test --tests "com.learnkk.messaging.*"` — 403/401/400 케이스 포함.
- 통합(Docker): `MessageIntegrationTest`의 stranger→403, admin→200 관통.

## 알려진 경계·이월

- FE `listRecipients`는 UX 보조일 뿐 권위 아님 — 서버 `canMessage`가 최종 판정(FE 조작으로 우회 불가).
- 첨부·리치텍스트 없음(plain text)로 저장 XSS 표면 최소. 실시간/DAST·SAST 자동화는 CI 스코프(이번 미실행).
