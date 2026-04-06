#!/usr/bin/env bash
set -e

export PGPASSWORD="${POSTGRES_PASSWORD:-nalbbun1234}"

DB_USER="${POSTGRES_USER:-nalbbun}"
DEFAULT_DB="${POSTGRES_DB:-nalbbun_ai}"

EXTRA_DBS=(
  "nalbbun_api"
  "nalbbun_vector"
  "nalbbun_memory"
)

for DB_NAME in "${EXTRA_DBS[@]}"; do
  EXISTS=$(psql -v ON_ERROR_STOP=1 --username "$DB_USER" --dbname postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")
  if [ "$EXISTS" != "1" ]; then
    echo "Creating database: ${DB_NAME}"
    psql -v ON_ERROR_STOP=1 --username "$DB_USER" --dbname postgres -c "CREATE DATABASE ${DB_NAME}"
  else
    echo "Database already exists: ${DB_NAME}"
  fi
done
