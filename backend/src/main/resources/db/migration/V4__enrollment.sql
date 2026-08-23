-- V4 enrollment: first-come-first-served applications with capacity / duplicate control (U4).

CREATE TABLE enrollment (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id   BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    mentee_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       varchar(20) NOT NULL DEFAULT 'APPLIED'
                 CHECK (status IN ('APPLIED', 'CANCELLED')),
    applied_at   timestamptz NOT NULL DEFAULT now(),
    cancelled_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz,
    -- One enrollment row per (meeting, mentee): the duplicate-application backstop (BR-U4-2).
    CONSTRAINT uq_enrollment_meeting_mentee UNIQUE (meeting_id, mentee_id)
);

-- Capacity count reads (meeting_id, status=APPLIED); applicant listing reads (meeting_id, status).
CREATE INDEX idx_enrollment_meeting_status ON enrollment (meeting_id, status);
-- "My enrollments" reads by mentee.
CREATE INDEX idx_enrollment_mentee ON enrollment (mentee_id);
