CREATE TABLE attachments (
    id                BIGSERIAL    PRIMARY KEY,
    message_id        BIGINT       REFERENCES messages(id) ON DELETE SET NULL,
    uploader_id       BIGINT       NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_path       VARCHAR(512) NOT NULL,
    file_size         BIGINT       NOT NULL,
    mime_type         VARCHAR(127),
    comment           VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_message_id ON attachments(message_id);
