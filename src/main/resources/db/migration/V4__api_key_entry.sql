CREATE TABLE IF NOT EXISTS api_key_entry (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    label VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    key_value TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_key_provider ON api_key_entry (provider, active);
