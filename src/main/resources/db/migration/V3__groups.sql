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
    left_at    DATE,
    created_at TIMESTAMP  NOT NULL,
    updated_at TIMESTAMP  NOT NULL
);

CREATE UNIQUE INDEX idx_group_members_one_active_per_user
    ON group_members (group_id, user_id)
    WHERE left_at IS NULL;

CREATE INDEX idx_group_members_group ON group_members (group_id);
CREATE INDEX idx_group_members_user  ON group_members (user_id);
