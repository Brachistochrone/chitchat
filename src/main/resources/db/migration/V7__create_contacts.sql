CREATE TABLE contacts (
    id           BIGSERIAL    PRIMARY KEY,
    requester_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    message      VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (requester_id, addressee_id)
);

CREATE INDEX idx_contacts_addressee_id ON contacts(addressee_id);
