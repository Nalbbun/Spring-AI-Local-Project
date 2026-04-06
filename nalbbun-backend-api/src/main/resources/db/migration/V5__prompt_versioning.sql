ALTER TABLE prompt_entry
    ADD COLUMN IF NOT EXISTS version_no INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS previous_version_id VARCHAR(36);

ALTER TABLE prompt_template
    ADD COLUMN IF NOT EXISTS version_no INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS previous_version_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_prompt_entry_prev_version ON prompt_entry(previous_version_id);
CREATE INDEX IF NOT EXISTS idx_prompt_template_prev_version ON prompt_template(previous_version_id);
