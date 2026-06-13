-- V2__users.sql
-- Creates the users table for authentication.
-- Separate from any group/expense data — authentication is its own concern.

CREATE TABLE users (
    id           BIGSERIAL    PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

-- Index on email since every login does a lookup by email
CREATE INDEX idx_users_email ON users (email);
