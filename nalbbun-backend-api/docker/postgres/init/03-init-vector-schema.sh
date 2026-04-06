#!/usr/bin/env bash
set -e
export PGPASSWORD="${POSTGRES_PASSWORD:-nalbbun1234}"
DB_USER="${POSTGRES_USER:-nalbbun}"
TARGET_DB="nalbbun_vector"
psql -v ON_ERROR_STOP=1 --username "$DB_USER" --dbname "$TARGET_DB" <<'SQL'
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    metadata JSON,
    embedding VECTOR(768)
);
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_source ON vector_store ((metadata->>'source'));
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_version ON vector_store ((metadata->>'version'));
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_category ON vector_store ((metadata->>'category'));
SQL
