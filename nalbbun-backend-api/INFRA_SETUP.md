# PostgreSQL + Redis 실행 가이드

## 1. 인프라 실행
```bash
cp .env.example .env
docker compose -f docker-compose.infra.yml --env-file .env up -d
```

## 2. JDBC 메모리로 실행
```bash
export APP_MEMORY_STORE=jdbc
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun
```

## 3. Redis 메모리로 실행
```bash
export APP_MEMORY_STORE=redis
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun
```

## 4. PostgreSQL 접속 확인
```bash
docker exec -it nalbbun-postgres psql -U nalbbun -d nalbbun_ai
```

## 5. Redis 접속 확인
```bash
docker exec -it nalbbun-redis redis-cli
```

## 메모리 저장소 선택 기준
- `in-memory`: 단일 개발 테스트
- `jdbc`: 영속 보관, 운영 기본값에 적합
- `redis`: 빠른 세션형 메모리, TTL 관리에 적합
 

## 메모리 저장소 선택값
- `APP_MEMORY_STORE=in-memory`
- `APP_MEMORY_STORE=jdbc`
- `APP_MEMORY_STORE=redis`

## PostgreSQL 사용 시
- `app.memory.store=jdbc`
- `spring.datasource.*` 설정 필요

## Redis 사용 시
- `app.memory.store=redis`
- `spring.data.redis.*` 설정 필요
- TTL: `app.memory.redis.ttl-hours`

실제 환경에서 검증 명령:
```bash
cp .env.example .env
docker compose -f docker-compose.infra.yml --env-file .env up -d
./gradlew clean test
./gradlew bootRun
```



## API DB / VECTOR DB 분리
- spring.datasource: RAG pgvector / embedding 용도
- app.api.datasource: API CRUD / Flyway / API key / Prompt / JDBC memory 용도
- spring.flyway.* 는 app.api.datasource 로 fallback 되도록 구성되었습니다.
- 메모리 저장소 전환 시 기존 대화는 자동 마이그레이션되지 않습니다.
