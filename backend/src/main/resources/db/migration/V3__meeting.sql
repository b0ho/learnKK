-- V3 meeting: meetings, survey_questions.

CREATE TABLE meetings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mentor_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           varchar(255) NOT NULL,
    topic           varchar(255),
    weeks           int          NOT NULL CHECK (weeks > 0),
    recruit_start   timestamptz,
    recruit_end     timestamptz,
    capacity        int          NOT NULL CHECK (capacity > 0),
    format          varchar(50),
    initial_content text,
    status          varchar(30)  NOT NULL DEFAULT 'PENDING_APPROVAL'
                     CHECK (status IN ('PENDING_APPROVAL', 'RECRUITING', 'READY_TO_START',
                                       'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    reject_reason   varchar(500),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz
);

CREATE INDEX idx_meetings_status ON meetings (status);
CREATE INDEX idx_meetings_mentor_id ON meetings (mentor_id);

CREATE TABLE survey_questions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id BIGINT       NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    order_no   int          NOT NULL,
    text       varchar(500) NOT NULL,
    type       varchar(30)  NOT NULL,
    options    text[],
    required   boolean      NOT NULL DEFAULT true
);

CREATE INDEX idx_survey_questions_meeting_id ON survey_questions (meeting_id);
