CREATE TABLE messages (
    id           BIGSERIAL    PRIMARY KEY,
    chat_type    VARCHAR(10)  NOT NULL CHECK (chat_type IN ('ROOM', 'PERSONAL')),
    room_id      BIGINT       REFERENCES rooms(id) ON DELETE SET NULL,
    sender_id    BIGINT       NOT NULL REFERENCES users(id),
    recipient_id BIGINT       REFERENCES users(id),
    content      VARCHAR(3072),
    reply_to_id  BIGINT       REFERENCES messages(id),
    edited_at    TIMESTAMPTZ,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_chat_type CHECK (
        (chat_type = 'ROOM'     AND room_id IS NOT NULL AND recipient_id IS NULL) OR
        (chat_type = 'PERSONAL' AND recipient_id IS NOT NULL AND room_id IS NULL)
    )
);

CREATE INDEX idx_messages_room     ON messages(room_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_personal ON messages(sender_id, recipient_id, created_at DESC) WHERE deleted_at IS NULL;
