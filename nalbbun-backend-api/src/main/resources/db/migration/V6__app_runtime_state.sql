CREATE TABLE IF NOT EXISTS app_runtime_state (
    id BIGINT PRIMARY KEY,
    active_memory_store VARCHAR(32) NOT NULL,
    requested_memory_store VARCHAR(32) NOT NULL,
    restart_requested_at TIMESTAMP NULL,
    last_applied_at TIMESTAMP NULL,
    redis_session_ttl_minutes INTEGER NOT NULL DEFAULT 180,
    last_action VARCHAR(64) NULL
);

INSERT INTO app_runtime_state(id, active_memory_store, requested_memory_store, redis_session_ttl_minutes, last_action)
VALUES (1, 'in-memory', 'in-memory', 180, 'BOOTSTRAP')
ON CONFLICT (id) DO NOTHING;
