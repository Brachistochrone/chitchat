CREATE TABLE users (
    id                              BIGSERIAL    PRIMARY KEY,
    email                           VARCHAR(255) NOT NULL UNIQUE,
    username                        VARCHAR(50)  NOT NULL UNIQUE,
    display_name                    VARCHAR(100),
    password_hash                   VARCHAR(255) NOT NULL,
    password_reset_token            VARCHAR(255),
    password_reset_token_expires_at TIMESTAMPTZ,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at                      TIMESTAMPTZ
);
