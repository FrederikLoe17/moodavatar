
-- V1__create_users.sql
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

CREATE TABLE users (
                       id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       username      VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          user_role    NOT NULL DEFAULT 'USER',
                       is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);