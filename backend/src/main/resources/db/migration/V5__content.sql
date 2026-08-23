-- V5 content: week posts, attachments (bytea BLOB) and notices (U6).
-- Ownership: U6 owns post / post_attachment / notice. meeting_id / author_id / uploader_id are
-- held by id only — participant/mentor authorization is resolved via U3/U4 Service reads, never by
-- crossing into their tables here.

CREATE TABLE post (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week       int         NOT NULL CHECK (week > 0),
    body       text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz
);

-- Post listing reads by (meeting_id, week) ordered for the week-by-week 자료실 view.
CREATE INDEX idx_post_meeting_week ON post (meeting_id, week);

CREATE TABLE post_attachment (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id      BIGINT       NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    file_name    varchar(255) NOT NULL,
    content_type varchar(150) NOT NULL,
    size_bytes   bigint       NOT NULL CHECK (size_bytes >= 0),
    data         bytea        NOT NULL,
    uploader_id  BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at   timestamptz  NOT NULL DEFAULT now()
);

-- Attachment metadata listing reads by post.
CREATE INDEX idx_post_attachment_post ON post_attachment (post_id);

CREATE TABLE notice (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meeting_id BIGINT      NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body       text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Notice listing reads by meeting, newest first.
CREATE INDEX idx_notice_meeting ON notice (meeting_id, created_at);
