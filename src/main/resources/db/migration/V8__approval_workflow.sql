










CREATE TABLE import_reviews (
    id                BIGSERIAL    PRIMARY KEY,
    import_job_id     BIGINT       NOT NULL UNIQUE,
    review_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    submitted_by      BIGINT       NOT NULL,
    submitted_at      TIMESTAMP    NOT NULL,
    reviewed_by       BIGINT,
    reviewed_at       TIMESTAMP,
    note              TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_review_import_job
        FOREIGN KEY (import_job_id) REFERENCES import_jobs(id) ON DELETE CASCADE
);

CREATE TABLE review_audit_logs (
    id               BIGSERIAL   PRIMARY KEY,
    import_review_id BIGINT      NOT NULL,
    import_job_id    BIGINT      NOT NULL,
    from_status      VARCHAR(20),                    
    to_status        VARCHAR(20) NOT NULL,
    actor_id         BIGINT      NOT NULL,
    note             TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_log_review
        FOREIGN KEY (import_review_id) REFERENCES import_reviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_job
        FOREIGN KEY (import_job_id) REFERENCES import_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_import_reviews_job_id ON import_reviews(import_job_id);
CREATE INDEX idx_review_audit_logs_review_id ON review_audit_logs(import_review_id);
CREATE INDEX idx_review_audit_logs_job_id ON review_audit_logs(import_job_id);
