-- Extensions are initialized in docker-entrypoint-initdb.d.
-- API CRUD tables are initialized by 02-app-api-schema.sql.
-- Vector store table is initialized by 03-vector-schema.sql.
-- Flyway migration files remain under src/main/resources/db/migration for idempotent API DB startup.
-- spring.datasource / app.vector.datasource should point to vector DB.
-- app.api.datasource / app.memory.jdbc.datasource can point to separate DBs.

-- POSTGRES_DB default is postgres; nalbbun_ai is no longer required in split-DB mode.
