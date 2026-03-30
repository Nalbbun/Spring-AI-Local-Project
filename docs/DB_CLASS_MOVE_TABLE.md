# DB 클래스 이동표

| 기존 위치 | 신규 위치 | 비고 |
|---|---|---|
| `config/JdbcMemoryDataSourceConfig` | `infra/db/config/PrimaryDataSourceConfig` | 공용 DataSource/JdbcTemplate 설정으로 역할 확대 |
| `domain/memory/service/JdbcConversationMemoryService` | `infra/db/memory/jdbc/JdbcConversationMemoryService` | JDBC 저장소 구현, 스키마 생성 제거 |
| `domain/memory/service/RedisConversationMemoryService` | `infra/db/memory/redis/RedisConversationMemoryService` | Redis 저장소 구현 분리 |
| `domain/prompt/repository/JdbcPromptRepository` | `infra/db/prompt/jdbc/JdbcPromptRepository` | JDBC 프롬프트 저장소 구현 분리 |
| `domain/prompt/repository/RedisPromptRepository` | `infra/db/prompt/redis/RedisPromptRepository` | Redis 프롬프트 저장소 구현 분리 |
| `domain/prompt/service/JdbcPromptTemplateService` | `infra/db/prompt/jdbc/JdbcPromptTemplateService` | JDBC 템플릿 저장소 구현 분리 |
| `infra/security/apikey/repository/ApiKeyRepository` | `domain/apikey/repository/ApiKeyRepository` + `infra/db/apikey/jdbc/JdbcApiKeyRepository` | 계약/구현 분리 |
| `docker/postgres/init/01-memory-schema.sql` | `src/main/resources/db/migration/V1__conversation_memory.sql` | 마이그레이션으로 이동 |
| `docker/postgres/init/02-prompt-template-schema.sql` | `src/main/resources/db/migration/V3__prompt_template.sql` | 마이그레이션으로 이동 |
```
