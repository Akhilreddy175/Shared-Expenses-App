-- V3__groups.sql
-- Groups and membership history tables.
--
-- Why "expense_groups" and not "groups"?
-- GROUP is a reserved keyword in SQL. Using it as a table name causes subtle
-- parse errors depending on the database driver version. expense_groups is
-- unambiguous and self-documenting.
--
-- Why joinedAt and leftAt instead of a deleted flag?
-- A deleted flag only tells you "this person left." It doesn't tell you WHEN.
-- Without the date, we can't answer: "Was Sam a member on April 10th?"
-- That question is critical for correctly attributing expenses.

CREATE TABLE expense_groups (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE group_members (
    id         BIGSERIAL  PRIMARY KEY,
    group_id   BIGINT     NOT NULL REFERENCES expense_groups(id),
    user_id    BIGINT     NOT NULL REFERENCES users(id),
    joined_at  DATE       NOT NULL,
    left_at    DATE,                   -- NULL means still active
    created_at TIMESTAMP  NOT NULL,
    updated_at TIMESTAMP  NOT NULL
);

-- This partial unique index is key to the design.
-- It allows a user to leave and rejoin (multiple rows with left_at set),
-- but prevents two simultaneously active memberships for the same user in the same group.
CREATE UNIQUE INDEX idx_group_members_one_active_per_user
    ON group_members (group_id, user_id)
    WHERE left_at IS NULL;

-- Lookup indexes for the two most common queries
CREATE INDEX idx_group_members_group ON group_members (group_id);
CREATE INDEX idx_group_members_user  ON group_members (user_id);
