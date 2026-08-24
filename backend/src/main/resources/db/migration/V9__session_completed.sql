-- V9 session completed: 멘토가 세션을 명시적으로 완료 처리할 수 있도록 완료 플래그 추가 (FR-8).
-- 세션 종료 판정 = completed=true (수동 완료) OR 시간창(scheduled_at + check_in_window_minutes) 경과(자동).
ALTER TABLE meeting_session ADD COLUMN completed boolean NOT NULL DEFAULT false;
