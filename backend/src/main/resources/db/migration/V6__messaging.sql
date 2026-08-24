-- V5 messaging: 1:1 direct-message threads with unread tracking (U7).

CREATE TABLE message_thread (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participant_a   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    participant_b   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_message_at timestamptz,
    updated_at      timestamptz,
    -- Participants are stored normalized (a < b) so a conversation maps to exactly one row.
    CONSTRAINT ck_message_thread_order CHECK (participant_a < participant_b),
    CONSTRAINT uq_message_thread_participants UNIQUE (participant_a, participant_b)
);

-- "My threads" reads by either participant.
CREATE INDEX idx_message_thread_a ON message_thread (participant_a);
CREATE INDEX idx_message_thread_b ON message_thread (participant_b);

CREATE TABLE message (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    thread_id  BIGINT      NOT NULL REFERENCES message_thread (id) ON DELETE CASCADE,
    sender_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body       text        NOT NULL,
    read_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Thread transcript reads by (thread, created_at); unread reads by (thread, sender, read_at).
CREATE INDEX idx_message_thread ON message (thread_id, created_at);
CREATE INDEX idx_message_unread ON message (thread_id, sender_id, read_at);
