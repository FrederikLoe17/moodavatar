-- V3: Notification schema

CREATE TABLE notifications (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    type          VARCHAR(50) NOT NULL,
    from_user_id  VARCHAR(255) NOT NULL,
    from_username VARCHAR(100) NOT NULL,
    read          BOOLEAN     NOT NULL DEFAULT FALSE,
    metadata      TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_read
    ON notifications (user_id, read, created_at DESC);
