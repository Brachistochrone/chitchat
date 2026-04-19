CREATE TABLE room_bans (
    room_id   BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by BIGINT      NOT NULL REFERENCES users(id),
    banned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);
