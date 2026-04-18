CREATE TABLE room_invites (
    id              BIGSERIAL   PRIMARY KEY,
    room_id         BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    invited_user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_by      BIGINT      NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, invited_user_id)
);
