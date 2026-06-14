CREATE TABLE expenses (
    id           BIGSERIAL      PRIMARY KEY,
    group_id     BIGINT         NOT NULL REFERENCES expense_groups(id),
    description  VARCHAR(500)   NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL DEFAULT 'INR',
    expense_date DATE           NOT NULL,
    paid_by      BIGINT         NOT NULL REFERENCES users(id),
    split_type   VARCHAR(20)    NOT NULL DEFAULT 'EQUAL',
    category     VARCHAR(100),
    created_by   BIGINT         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    CONSTRAINT expenses_amount_positive CHECK (amount > 0)
);

CREATE TABLE expense_participants (
    id           BIGSERIAL      PRIMARY KEY,
    expense_id   BIGINT         NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id      BIGINT         NOT NULL REFERENCES users(id),
    share_amount NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    CONSTRAINT participants_share_positive  CHECK (share_amount >= 0),
    UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expenses_group       ON expenses (group_id);
CREATE INDEX idx_expenses_paid_by     ON expenses (paid_by);
CREATE INDEX idx_expenses_date        ON expenses (expense_date);
CREATE INDEX idx_participants_expense ON expense_participants (expense_id);
CREATE INDEX idx_participants_user    ON expense_participants (user_id);
