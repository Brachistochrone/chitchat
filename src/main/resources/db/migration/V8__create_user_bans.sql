CREATE TABLE user_bans (
    id         BIGSERIAL   PRIMARY KEY,
    banner_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (banner_id, banned_id)
);
