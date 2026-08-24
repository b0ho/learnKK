# Unit Test Instructions — Bolt 5 Messaging (Standard)

<!-- 상류: bolt5-messaging code-generation-plan.md (Step 7) · code-summary.md. -->

## 프레임워크·실행

- **Backend**: JUnit 5 + Mockito + AssertJ(서비스), `@WebMvcTest` + MockMvc(웹).
  - 서비스: `cd backend && ./gradlew test --tests "com.learnkk.messaging.service.*"`
  - 웹: `./gradlew test --tests "com.learnkk.messaging.web.*"`
  - 계약: `./gradlew test --tests "com.learnkk.contract.*"`
- **Frontend**: Vitest + React Testing Library.
  - `cd frontend && npx vitest run src/features/messaging src/routes/AppShell.test.tsx`

## 커버리지 대상 (Standard: 컴포넌트당 5~8)

- **MessageServiceTest** — send: 자기발신(400)·빈본문(400)·상대없음(404)·멘토→활성멘티(성공)·멘토→무관멘티(403)·멘티→신청모임멘토(성공)·멘티→무관멘토(403)·관리자↔전원(성공)·기존 스레드 재사용; getThread: 비참여자(403)·미존재(404)·참여자 열람+확인처리; unreadCount; listThreads; listRecipients(멘토).
- **MessageControllerTest** — 라우트별 201/401(무토큰)/403/400(빈본문 bean-validation).
- **FE** — MessagesPage(스레드목록·미확인뱃지·빈상태·발신·네비게이션), ThreadView(내/상대 구분·답장·403), AppShell(미확인 뱃지 표시/숨김).

## 커버리지 목표

- BE/FE 각 **LINE ≥ 80%**. 실측: 전체 BE LINE 86.6%, FE 94.87%(messaging 91.9%, messages.ts 100%) — `build-test-results.md`.
