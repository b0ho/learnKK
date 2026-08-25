-- V11 seed: 데모/개발용 멘토 2명 + 멘티 7명.
--
-- V10에서 시드한 관리자(admin)와 함께 시드 계정 총합은 어드민1 + 멘토2 + 멘티7 = 10명.
-- password_hash는 Spring BCryptPasswordEncoder와 호환되는 bcrypt($2a$, strength 10)이며
-- 이식성을 위해 미리 계산한 값을 하드코딩한다(pgcrypto 확장 불필요).
--
-- 시드 계정 공통 비밀번호: password123  (개발용 — 운영 배포 전 반드시 정리/변경할 것)
-- 재실행/중복에 안전하도록 ON CONFLICT DO NOTHING 사용.

INSERT INTO users (nickname, password_hash, employee_no, role) VALUES
  ('멘토1', '$2a$10$6KtAnWQ5mQNDSQgtWxjEEeAnIlkjquiEBkBzxSGUBAz4CvA3bQL0.', 'MENTOR001', 'MENTOR'),
  ('멘토2', '$2a$10$Ts4oJB7a6y4IlZmv5HiZ1.sLpGX59FCdoO5ozW7PDYRaOCZ3aYP/m', 'MENTOR002', 'MENTOR'),
  ('멘티1', '$2a$10$aYsBJF3ueSyqeSB3o7H4E.Rj1iHq69n.1NLYoV./y6vTZnxZM4JEG', 'MENTEE001', 'MENTEE'),
  ('멘티2', '$2a$10$GX0X9P2fm/3Qo36S.XiDtuqjGmXQgtO0GGaAUv.r5cO.IDTrnERB2', 'MENTEE002', 'MENTEE'),
  ('멘티3', '$2a$10$zc8O5eAR1bFDfcz3IE2bae7yVUpMFOpkXtp0C0VGTs8MbpEHY/nZC', 'MENTEE003', 'MENTEE'),
  ('멘티4', '$2a$10$l0R1sZljkCNhMlKxVXRNDu.6.rKww15qTTkt1hfHtQcXoc8naLN4a', 'MENTEE004', 'MENTEE'),
  ('멘티5', '$2a$10$idTkmt2XDvxWr6Kh0H/KfeDrmscH2ovdeulJtdfsGtK6jrznv5Aii', 'MENTEE005', 'MENTEE'),
  ('멘티6', '$2a$10$.rh0DfFBCe2QwcJA7vdared3Mml6Jt2FkfALraIcMCCmptwNdigE6', 'MENTEE006', 'MENTEE'),
  ('멘티7', '$2a$10$OXluirC/qUmHDQEWyPIkA.qKl63l9QZxMTVSSZD7EmI1fQ3Ib0Zk2', 'MENTEE007', 'MENTEE')
ON CONFLICT (nickname) DO NOTHING;

INSERT INTO profiles (user_id)
SELECT id FROM users
WHERE employee_no IN (
  'MENTOR001', 'MENTOR002',
  'MENTEE001', 'MENTEE002', 'MENTEE003', 'MENTEE004', 'MENTEE005', 'MENTEE006', 'MENTEE007'
)
ON CONFLICT (user_id) DO NOTHING;
