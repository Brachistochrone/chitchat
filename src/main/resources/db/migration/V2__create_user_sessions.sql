CREATE TABLE user_sessions (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    browser      VARCHAR(255),
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
