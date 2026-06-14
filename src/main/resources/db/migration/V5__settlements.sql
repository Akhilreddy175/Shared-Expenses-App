















CREATE TABLE settlements (
    id              BIGSERIAL      PRIMARY KEY,
    group_id        BIGINT         NOT NULL REFERENCES expense_groups(id),
    payer_id        BIGINT         NOT NULL REFERENCES users(id),
    receiver_id     BIGINT         NOT NULL REFERENCES users(id),
    amount          NUMERIC(12, 2) NOT NULL,
    settlement_date DATE           NOT NULL,
    note            VARCHAR(500),
    created_by      BIGINT         NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT settlements_amount_positive   CHECK (amount > 0),
    CONSTRAINT settlements_different_users   CHECK (payer_id != receiver_id)
);

CREATE INDEX idx_settlements_group    ON settlements (group_id);
CREATE INDEX idx_settlements_payer    ON settlements (payer_id);
CREATE INDEX idx_settlements_receiver ON settlements (receiver_id);
