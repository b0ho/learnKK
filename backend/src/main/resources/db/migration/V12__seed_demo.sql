-- V12 seed: 데모/테스트용 다양한 케이스의 초기 데이터 (ux-bugfixes-2 FR-11).
--
-- V10(admin)·V11(멘토2·멘티7)에서 시드한 계정 위에, 모든 모임 상태와 다양한 참여/출석/수료/설문/피드백
-- 케이스를 담은 모임을 등록한다. 개발용 데모 데이터이며 운영 배포 전 정리 대상이다.
--
-- 멱등 원칙: 모임/세션/문항은 자연키(제목·주차·순번) NOT EXISTS 가드, 유니크 제약이 있는
-- 신청/출석/응답/피드백/수료는 ON CONFLICT DO NOTHING. 자동 생성 id 는 하드코딩하지 않고 SELECT 로 결합한다.
-- 시드 계정 공통 비밀번호: password123.

-- ---------------------------------------------------------------------------
-- 모임 (모든 상태를 고르게 포함)
--   멘토1(MENTOR001): 리액트 실전 스터디(IN_PROGRESS), 완료된 CS 스터디(COMPLETED), 모집중 알고리즘(RECRUITING)
--   멘토2(MENTOR002): 개설 대기 코틀린(PENDING_APPROVAL), 시작 대기 도커(READY_TO_START),
--                     정원 마감 파이썬(RECRUITING·마감), 반려된 블록체인(REJECTED), 취소된 UX(CANCELLED)
-- ---------------------------------------------------------------------------

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '리액트 실전 스터디', 'Frontend', 6, now() - interval '30 day', now() - interval '20 day', 6, 'ONLINE', 'IN_PROGRESS', NULL, now() - interval '35 day'
FROM users u WHERE u.employee_no = 'MENTOR001'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '리액트 실전 스터디');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '완료된 CS 스터디', 'Computer Science', 4, now() - interval '80 day', now() - interval '70 day', 5, 'OFFLINE', 'COMPLETED', NULL, now() - interval '85 day'
FROM users u WHERE u.employee_no = 'MENTOR001'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '완료된 CS 스터디');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '모집중 알고리즘', 'Algorithm', 8, now() - interval '3 day', now() + interval '10 day', 6, 'ONLINE', 'RECRUITING', NULL, now() - interval '5 day'
FROM users u WHERE u.employee_no = 'MENTOR001'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '모집중 알고리즘');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '개설 대기 코틀린', 'Kotlin', 5, NULL, NULL, 4, 'ONLINE', 'PENDING_APPROVAL', NULL, now() - interval '1 day'
FROM users u WHERE u.employee_no = 'MENTOR002'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '개설 대기 코틀린');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '시작 대기 도커', 'DevOps', 4, now() - interval '15 day', now() - interval '2 day', 5, 'ONLINE', 'READY_TO_START', NULL, now() - interval '18 day'
FROM users u WHERE u.employee_no = 'MENTOR002'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '시작 대기 도커');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '정원 마감 파이썬', 'Python', 6, now() - interval '4 day', now() + interval '7 day', 2, 'ONLINE', 'RECRUITING', NULL, now() - interval '6 day'
FROM users u WHERE u.employee_no = 'MENTOR002'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '정원 마감 파이썬');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '반려된 블록체인', 'Blockchain', 10, NULL, NULL, 8, 'ONLINE', 'REJECTED', '주제 범위가 과정 취지와 맞지 않습니다.', now() - interval '12 day'
FROM users u WHERE u.employee_no = 'MENTOR002'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '반려된 블록체인');

INSERT INTO meetings (mentor_id, title, topic, weeks, recruit_start, recruit_end, capacity, format, status, reject_reason, created_at)
SELECT u.id, '취소된 UX 워크숍', 'Design', 3, now() - interval '20 day', now() - interval '10 day', 6, 'OFFLINE', 'CANCELLED', '신청 인원 부족으로 모집이 취소되었습니다.', now() - interval '22 day'
FROM users u WHERE u.employee_no = 'MENTOR002'
  AND NOT EXISTS (SELECT 1 FROM meetings m WHERE m.title = '취소된 UX 워크숍');

-- ---------------------------------------------------------------------------
-- 신청 (APPLIED / CANCELLED, 정원 여유/마감)
-- ---------------------------------------------------------------------------

-- 모집중 알고리즘: 멘티1·2·3 신청, 멘티4 취소(정원 여유)
INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at, cancelled_at)
SELECT m.id, u.id, 'APPLIED', now() - interval '2 day', NULL
FROM meetings m, users u WHERE m.title = '모집중 알고리즘' AND u.employee_no IN ('MENTEE001','MENTEE002','MENTEE003')
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at, cancelled_at)
SELECT m.id, u.id, 'CANCELLED', now() - interval '3 day', now() - interval '1 day'
FROM meetings m, users u WHERE m.title = '모집중 알고리즘' AND u.employee_no = 'MENTEE004'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- 정원 마감 파이썬(정원 2): 멘티5·6 신청 → 마감
INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at)
SELECT m.id, u.id, 'APPLIED', now() - interval '3 day'
FROM meetings m, users u WHERE m.title = '정원 마감 파이썬' AND u.employee_no IN ('MENTEE005','MENTEE006')
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- 시작 대기 도커: 멘티1·2 신청
INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at)
SELECT m.id, u.id, 'APPLIED', now() - interval '10 day'
FROM meetings m, users u WHERE m.title = '시작 대기 도커' AND u.employee_no IN ('MENTEE001','MENTEE002')
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- 리액트 실전 스터디(IN_PROGRESS): 멘티1·2·3 참여
INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at)
SELECT m.id, u.id, 'APPLIED', now() - interval '25 day'
FROM meetings m, users u WHERE m.title = '리액트 실전 스터디' AND u.employee_no IN ('MENTEE001','MENTEE002','MENTEE003')
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- 완료된 CS 스터디(COMPLETED): 멘티1·2·4 참여
INSERT INTO enrollment (meeting_id, mentee_id, status, applied_at)
SELECT m.id, u.id, 'APPLIED', now() - interval '75 day'
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no IN ('MENTEE001','MENTEE002','MENTEE004')
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 세션 (리액트 실전: 2 과거+1 미래 / CS: 4 과거·완료)
-- ---------------------------------------------------------------------------

INSERT INTO meeting_session (meeting_id, week, scheduled_at, check_in_window_minutes, completed)
SELECT m.id, 1, now() - interval '14 day', 120, true FROM meetings m WHERE m.title = '리액트 실전 스터디'
  AND NOT EXISTS (SELECT 1 FROM meeting_session s WHERE s.meeting_id = m.id AND s.week = 1);
INSERT INTO meeting_session (meeting_id, week, scheduled_at, check_in_window_minutes, completed)
SELECT m.id, 2, now() - interval '7 day', 120, true FROM meetings m WHERE m.title = '리액트 실전 스터디'
  AND NOT EXISTS (SELECT 1 FROM meeting_session s WHERE s.meeting_id = m.id AND s.week = 2);
INSERT INTO meeting_session (meeting_id, week, scheduled_at, check_in_window_minutes, completed)
SELECT m.id, 3, now() + interval '3 day', 120, false FROM meetings m WHERE m.title = '리액트 실전 스터디'
  AND NOT EXISTS (SELECT 1 FROM meeting_session s WHERE s.meeting_id = m.id AND s.week = 3);

INSERT INTO meeting_session (meeting_id, week, scheduled_at, check_in_window_minutes, completed)
SELECT m.id, g.w, now() - interval '60 day' + (g.w || ' week')::interval, 120, true
FROM meetings m, generate_series(1,4) AS g(w) WHERE m.title = '완료된 CS 스터디'
  AND NOT EXISTS (SELECT 1 FROM meeting_session s WHERE s.meeting_id = m.id AND s.week = g.w);

-- ---------------------------------------------------------------------------
-- 출석 (부분/전체) — 세션 id 는 (제목·주차)로 결합
-- 리액트: 멘티1 = 1·2주차 모두, 멘티2 = 1주차만, 멘티3 = 없음
-- ---------------------------------------------------------------------------

INSERT INTO attendance (session_id, mentee_id, checked_in_at)
SELECT s.id, u.id, s.scheduled_at + interval '10 minute'
FROM meeting_session s JOIN meetings m ON s.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND s.week IN (1,2) AND u.employee_no = 'MENTEE001'
ON CONFLICT (session_id, mentee_id) DO NOTHING;

INSERT INTO attendance (session_id, mentee_id, checked_in_at)
SELECT s.id, u.id, s.scheduled_at + interval '10 minute'
FROM meeting_session s JOIN meetings m ON s.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND s.week = 1 AND u.employee_no = 'MENTEE002'
ON CONFLICT (session_id, mentee_id) DO NOTHING;

-- CS 스터디: 멘티1 = 4/4(수료 확정), 멘티2 = 4/4(수료 후보), 멘티4 = 1/4(미수료)
INSERT INTO attendance (session_id, mentee_id, checked_in_at)
SELECT s.id, u.id, s.scheduled_at + interval '5 minute'
FROM meeting_session s JOIN meetings m ON s.meeting_id = m.id, users u
WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE001'
ON CONFLICT (session_id, mentee_id) DO NOTHING;

INSERT INTO attendance (session_id, mentee_id, checked_in_at)
SELECT s.id, u.id, s.scheduled_at + interval '5 minute'
FROM meeting_session s JOIN meetings m ON s.meeting_id = m.id, users u
WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE002'
ON CONFLICT (session_id, mentee_id) DO NOTHING;

INSERT INTO attendance (session_id, mentee_id, checked_in_at)
SELECT s.id, u.id, s.scheduled_at + interval '5 minute'
FROM meeting_session s JOIN meetings m ON s.meeting_id = m.id, users u
WHERE m.title = '완료된 CS 스터디' AND s.week = 1 AND u.employee_no = 'MENTEE004'
ON CONFLICT (session_id, mentee_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 수료 판정 (완료된 CS 스터디) — 후보/확정/미수료
-- ---------------------------------------------------------------------------

INSERT INTO mentee_completion (meeting_id, mentee_id, status, attended_count, total_scheduled, approved_at)
SELECT m.id, u.id, 'COMPLETED', 4, 4, now() - interval '2 day'
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE001'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- 멘티2 = 4/4(수료 후보): 80% 규칙(a*100 >= 80*S) 충족(400 >= 320). 확정(멘티1)과 판정 상태만 다르다.
INSERT INTO mentee_completion (meeting_id, mentee_id, status, attended_count, total_scheduled, approved_at)
SELECT m.id, u.id, 'COMPLETION_CANDIDATE', 4, 4, NULL
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE002'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

INSERT INTO mentee_completion (meeting_id, mentee_id, status, attended_count, total_scheduled, approved_at)
SELECT m.id, u.id, 'NOT_COMPLETED', 1, 4, NULL
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE004'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 사전설문 문항 + 응답 (리액트 실전: 2문항, 멘티1·2 응답)
-- ---------------------------------------------------------------------------

INSERT INTO survey_questions (meeting_id, order_no, text, type, options, required)
SELECT m.id, 1, '참여 동기를 알려주세요.', 'LONG_TEXT', NULL, true FROM meetings m WHERE m.title = '리액트 실전 스터디'
  AND NOT EXISTS (SELECT 1 FROM survey_questions q WHERE q.meeting_id = m.id AND q.order_no = 1);
INSERT INTO survey_questions (meeting_id, order_no, text, type, options, required)
SELECT m.id, 2, '현재 실력 수준은?', 'CHOICE', ARRAY['초급','중급','고급'], true FROM meetings m WHERE m.title = '리액트 실전 스터디'
  AND NOT EXISTS (SELECT 1 FROM survey_questions q WHERE q.meeting_id = m.id AND q.order_no = 2);

INSERT INTO survey_answer (meeting_id, question_id, mentee_id, answer_text)
SELECT m.id, q.id, u.id, '실무 프로젝트에 바로 적용하고 싶어서 신청했습니다.'
FROM meetings m JOIN survey_questions q ON q.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND q.order_no = 1 AND u.employee_no = 'MENTEE001'
ON CONFLICT (question_id, mentee_id) DO NOTHING;
INSERT INTO survey_answer (meeting_id, question_id, mentee_id, answer_text)
SELECT m.id, q.id, u.id, '중급'
FROM meetings m JOIN survey_questions q ON q.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND q.order_no = 2 AND u.employee_no = 'MENTEE001'
ON CONFLICT (question_id, mentee_id) DO NOTHING;
INSERT INTO survey_answer (meeting_id, question_id, mentee_id, answer_text)
SELECT m.id, q.id, u.id, '기초를 탄탄히 다지고 싶습니다.'
FROM meetings m JOIN survey_questions q ON q.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND q.order_no = 1 AND u.employee_no = 'MENTEE002'
ON CONFLICT (question_id, mentee_id) DO NOTHING;
INSERT INTO survey_answer (meeting_id, question_id, mentee_id, answer_text)
SELECT m.id, q.id, u.id, '초급'
FROM meetings m JOIN survey_questions q ON q.meeting_id = m.id, users u
WHERE m.title = '리액트 실전 스터디' AND q.order_no = 2 AND u.employee_no = 'MENTEE002'
ON CONFLICT (question_id, mentee_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 과정 피드백 (완료된 CS 스터디: 멘티1·2)
-- ---------------------------------------------------------------------------

INSERT INTO feedback (meeting_id, mentee_id, content, created_at)
SELECT m.id, u.id, '커리큘럼이 체계적이라 큰 도움이 되었습니다. 감사합니다!', now() - interval '3 day'
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE001'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;
INSERT INTO feedback (meeting_id, mentee_id, content, created_at)
SELECT m.id, u.id, '실습 위주라 이해가 빨랐습니다. 다음 기수도 기대돼요.', now() - interval '3 day'
FROM meetings m, users u WHERE m.title = '완료된 CS 스터디' AND u.employee_no = 'MENTEE002'
ON CONFLICT (meeting_id, mentee_id) DO NOTHING;
