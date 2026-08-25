-- V10 seed: 초기 관리자(ADMIN) 계정.
--
-- ADMIN은 signup API로 가입할 수 없으므로(ADMIN_SIGNUP_FORBIDDEN) 최초 관리자는 여기서 시드한다.
-- password_hash는 Spring BCryptPasswordEncoder와 호환되는 bcrypt($2a$, strength 10) 해시이며
-- 이식성을 위해 미리 계산한 값을 하드코딩한다(pgcrypto 확장 불필요).
--
-- 기본 자격증명 (최초 로그인 후 반드시 비밀번호 변경할 것):
--   nickname    : admin
--   employee_no : ADMIN001
--   password    : admin1234
--
-- 재실행/중복에 안전하도록 ON CONFLICT DO NOTHING 사용.

INSERT INTO users (nickname, password_hash, employee_no, role)
VALUES ('admin', '$2a$10$iZ6lt3mEaki6PW3lyjKdk.sNVxv9EDKYGyE5/5YCTN0QQJ.ShKX8e', 'ADMIN001', 'ADMIN')
ON CONFLICT (nickname) DO NOTHING;

INSERT INTO profiles (user_id)
SELECT id FROM users WHERE nickname = 'admin'
ON CONFLICT (user_id) DO NOTHING;
