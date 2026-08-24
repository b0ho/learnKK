-- V5 session: 세션 일정 · 팝업 출석(시간창) · 멘티 수료 판정 (U5).
-- auth 토큰 테이블 sessions(V2)와 이름 충돌을 피하기 위해 세션 테이블명은 meeting_session.

CREATE TABLE meeting_session (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id               BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    week                     int         NOT NULL,
    scheduled_at             timestamptz NOT NULL,
    -- 출석 유효 시간창 길이(분). 기본 120분(ADR-005, business-rules BR-U5-2).
    check_in_window_minutes  int         NOT NULL DEFAULT 120,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz
);

-- 세션 목록/출석율/게이트 read 는 meeting_id 로 조회.
CREATE INDEX idx_meeting_session_meeting ON meeting_session (meeting_id);

CREATE TABLE attendance (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id    BIGINT      NOT NULL REFERENCES meeting_session (id) ON DELETE CASCADE,
    mentee_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    checked_in_at timestamptz NOT NULL DEFAULT now(),
    created_at    timestamptz NOT NULL DEFAULT now(),
    -- 세션당 멘티 1회: 멱등 백스톱(BR-U5-2).
    CONSTRAINT uq_attendance_session_mentee UNIQUE (session_id, mentee_id)
);

-- 멘티별 출석 집계 조회.
CREATE INDEX idx_attendance_mentee ON attendance (mentee_id);

CREATE TABLE mentee_completion (
    meeting_id      BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    mentee_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status          varchar(30) NOT NULL DEFAULT 'NOT_COMPLETED'
                    CHECK (status IN ('NOT_COMPLETED', 'COMPLETION_CANDIDATE', 'COMPLETED')),
    -- 판정 근거 스냅샷(a / S).
    attended_count  int         NOT NULL DEFAULT 0,
    total_scheduled int         NOT NULL DEFAULT 0,
    approved_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz,
    CONSTRAINT pk_mentee_completion PRIMARY KEY (meeting_id, mentee_id)
);
