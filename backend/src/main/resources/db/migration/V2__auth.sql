-- V2 auth: users, profiles, sessions.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nickname      varchar(50)  NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    employee_no   varchar(50)  NOT NULL UNIQUE,
    role          varchar(20)  NOT NULL CHECK (role IN ('MENTOR', 'MENTEE', 'ADMIN')),
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz
);

CREATE TABLE profiles (
    user_id       BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    interest_tags text[],
    intro         varchar(500)
);

CREATE TABLE sessions (
    token      varchar(255) PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       varchar(20)  NOT NULL CHECK (role IN ('MENTOR', 'MENTEE', 'ADMIN')),
    created_at timestamptz  NOT NULL DEFAULT now(),
    expires_at timestamptz  NOT NULL,
    revoked_at timestamptz
);

CREATE INDEX idx_sessions_user_id ON sessions (user_id);
