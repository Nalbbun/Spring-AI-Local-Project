CREATE TABLE IF NOT EXISTS prompt_entry (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(32),
    system_prompt TEXT NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prompt_category ON prompt_entry (category);
CREATE INDEX IF NOT EXISTS idx_prompt_default ON prompt_entry (category, is_default);
