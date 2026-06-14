












CREATE TABLE import_jobs (
    id              BIGSERIAL       PRIMARY KEY,
    group_id        BIGINT          NOT NULL REFERENCES expense_groups(id),
    filename        VARCHAR(255)    NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PROCESSING',
    total_rows      INT             NOT NULL DEFAULT 0,
    valid_rows      INT             NOT NULL DEFAULT 0,
    invalid_rows    INT             NOT NULL DEFAULT 0,
    imported_rows   INT             NOT NULL DEFAULT 0,
    error_message   VARCHAR(500),   
    uploaded_by     BIGINT          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_import_jobs_group ON import_jobs (group_id);

CREATE TABLE import_rows (
    id                  BIGSERIAL       PRIMARY KEY,
    import_job_id       BIGINT          NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number          INT             NOT NULL,
    raw_data            TEXT            NOT NULL,       
    status              VARCHAR(20)     NOT NULL DEFAULT 'VALID',
    
    description         VARCHAR(500),
    amount              NUMERIC(12, 2),
    currency            VARCHAR(10),
    expense_date        DATE,
    paid_by_name        VARCHAR(255),
    paid_by_user_id     BIGINT,                        
    split_type          VARCHAR(20),
    category            VARCHAR(100),
    participants_raw    TEXT,                           
    participant_ids     TEXT,                           
    participant_values  TEXT,                           
    created_expense_id  BIGINT,                        
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);

CREATE INDEX idx_import_rows_job        ON import_rows (import_job_id);
CREATE INDEX idx_import_rows_job_status ON import_rows (import_job_id, status);

CREATE TABLE import_issues (
    id              BIGSERIAL       PRIMARY KEY,
    import_row_id   BIGINT          NOT NULL REFERENCES import_rows(id) ON DELETE CASCADE,
    issue_type      VARCHAR(50)     NOT NULL,  
    severity        VARCHAR(10)     NOT NULL,  
    field_name      VARCHAR(100),              
    raw_value       VARCHAR(500),              
    message         TEXT            NOT NULL,  
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX idx_import_issues_row ON import_issues (import_row_id);
