-- V5__settlements.sql
--
-- Why is Settlement a separate table from expenses?
--
-- An expense represents money spent on something shared: food, rent, utilities.
-- It has a split strategy, participants, and creates individual share obligations.
--
-- A settlement represents cash that directly changed hands between two people
-- to wipe out a debt. It has no split, no participants, no category.
-- It's not "spending" — it's "paying back."
--
-- If we modelled settlements as expenses, we would need to fake a split type,
-- invent a category, and confuse the balance engine with "expenses" that
-- don't actually generate new obligations. The two concepts are fundamentally different.
-- Keeping them separate makes each table's purpose unambiguous.

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
