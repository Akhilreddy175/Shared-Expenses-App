CREATE TABLE IF NOT EXISTS application_settings (
    key   VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL,
    description VARCHAR(500)
);

INSERT INTO application_settings (key, value, description)
VALUES ('usd_to_inr_rate', '83.50', 'Exchange rate used for USD-to-INR conversion in balance calculations');
