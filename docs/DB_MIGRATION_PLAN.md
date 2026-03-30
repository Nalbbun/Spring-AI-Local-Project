# 테이블/마이그레이션 분리안

## 원칙
- Docker init SQL: 확장 설치 등 인프라 준비만 담당
- 애플리케이션 테이블/인덱스: `src/main/resources/db/migration`에서 관리
- Java 코드 내부 `CREATE TABLE IF NOT EXISTS`: 제거

## 마이그레이션 파일
- `V1__conversation_memory.sql`
  - `conversation_message`
  - `conversation_summary`
  - `conversation_note`
- `V2__prompt_entry.sql`
  - `prompt_entry`
- `V3__prompt_template.sql`
  - `prompt_template`
- `V4__api_key_entry.sql`
  - `api_key_entry`

## 실행 방법
1. PostgreSQL/pgvector 컨테이너 기동
2. 애플리케이션 실행 전 Flyway 또는 수동 SQL 적용
3. 애플리케이션은 저장/조회만 수행
