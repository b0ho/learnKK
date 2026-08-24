-- V5 survey/feedback (U8): pre-application survey answers and course feedback.
-- Question templates (survey_questions) are owned by U3; U8 owns only the answers and feedback.

CREATE TABLE survey_answer (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id  BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    question_id BIGINT      NOT NULL REFERENCES survey_questions (id) ON DELETE CASCADE,
    mentee_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    answer_text text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    -- One answer row per (question, mentee): re-submission updates in place (BR-U8-1).
    CONSTRAINT uq_survey_answer_question_mentee UNIQUE (question_id, mentee_id)
);

-- Answer listing reads by meeting (getAnswers / mentee-scoped read).
CREATE INDEX idx_survey_answer_meeting ON survey_answer (meeting_id);
CREATE INDEX idx_survey_answer_meeting_mentee ON survey_answer (meeting_id, mentee_id);

CREATE TABLE feedback (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    mentee_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content    text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    -- One feedback row per (meeting, mentee): re-submission updates in place (BR-U8-3).
    CONSTRAINT uq_feedback_meeting_mentee UNIQUE (meeting_id, mentee_id)
);

-- Feedback listing reads by meeting (owning mentor / admin).
CREATE INDEX idx_feedback_meeting ON feedback (meeting_id);
