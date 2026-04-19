CREATE TABLE unread_counts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id      BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    chat_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    count        INT    NOT NULL DEFAULT 0,
    CONSTRAINT chk_unread_type CHECK (
        (room_id IS NOT NULL AND chat_user_id IS NULL) OR
        (chat_user_id IS NOT NULL AND room_id IS NULL)
    ),
    CONSTRAINT uq_unread_room UNIQUE (user_id, room_id),
    CONSTRAINT uq_unread_chat UNIQUE (user_id, chat_user_id)
);
