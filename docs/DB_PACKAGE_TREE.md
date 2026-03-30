# DB 분리용 실제 폴더 트리

```text
src/main/java/ai/local/nalbbun
├─ domain
│  ├─ apikey
│  │  └─ repository
│  │     └─ ApiKeyRepository.java
│  ├─ memory
│  │  ├─ model
│  │  └─ service
│  │     └─ ConversationMemoryService.java
│  └─ prompt
│     ├─ model
│     ├─ repository
│     │  └─ PromptRepository.java
│     └─ service
│        └─ PromptTemplateService.java
└─ infra
   └─ db
      ├─ config
      │  └─ PrimaryDataSourceConfig.java
      ├─ apikey
      │  └─ jdbc
      │     └─ JdbcApiKeyRepository.java
      ├─ memory
      │  ├─ jdbc
      │  │  └─ JdbcConversationMemoryService.java
      │  └─ redis
      │     └─ RedisConversationMemoryService.java
      └─ prompt
         ├─ jdbc
         │  ├─ JdbcPromptRepository.java
         │  └─ JdbcPromptTemplateService.java
         └─ redis
            └─ RedisPromptRepository.java

src/main/resources
└─ db
   └─ migration
      ├─ V1__conversation_memory.sql
      ├─ V2__prompt_entry.sql
      ├─ V3__prompt_template.sql
      └─ V4__api_key_entry.sql
```
