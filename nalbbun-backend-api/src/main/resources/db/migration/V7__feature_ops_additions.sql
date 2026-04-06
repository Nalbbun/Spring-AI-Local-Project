
ALTER TABLE app_runtime_state
    ADD COLUMN IF NOT EXISTS redis_memory_ttl_minutes INTEGER NOT NULL DEFAULT 1440;

CREATE TABLE IF NOT EXISTS prompt_entry_history (
    history_id BIGSERIAL PRIMARY KEY,
    prompt_id VARCHAR(36) NOT NULL,
    action VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NULL,
    system_prompt TEXT NOT NULL,
    description TEXT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version_no INTEGER NOT NULL DEFAULT 1,
    previous_version_id VARCHAR(36) NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_prompt_entry_history_prompt_id ON prompt_entry_history(prompt_id, captured_at DESC);

CREATE TABLE IF NOT EXISTS memory_snapshot (
    snapshot_id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(128) NOT NULL,
    label VARCHAR(255) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_memory_snapshot_conversation_id ON memory_snapshot(conversation_id, created_at DESC);
