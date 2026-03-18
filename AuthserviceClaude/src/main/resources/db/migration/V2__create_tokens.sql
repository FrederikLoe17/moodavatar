
CREATE TABLE refresh_tokens (
                                id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token      VARCHAR(512) NOT NULL UNIQUE,
                                expires_at TIMESTAMP    NOT NULL,
                                created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
                                revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE password_resets (
                                 id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 token      VARCHAR(512) NOT NULL UNIQUE,
                                 expires_at TIMESTAMP    NOT NULL,
                                 used       BOOLEAN      NOT NULL DEFAULT FALSE
);