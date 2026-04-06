package ai.local.nalbbun.infra.config;

import ai.local.nalbbun.domain.prompt.model.PromptProperties;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;
import ai.local.nalbbun.infra.db.prompt.inmemory.InMemoryPromptRepository;
import ai.local.nalbbun.infra.db.prompt.jdbc.JdbcPromptRepository;
import ai.local.nalbbun.infra.db.prompt.redis.RedisPromptRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 프롬프트 저장소 선택 구성이다.
 * 기본값은 JDBC(API DB)이며, redis 를 명시한 경우에만 Redis 를 사용한다.
 * JDBC가 불가능한 환경에서는 인메모리로 자동 fallback 된다.
 */
@Configuration
public class PromptRepositoryConfig {

    @Bean
    public PromptRepository promptRepository(
            PromptProperties promptProperties,
            @Qualifier("apiJdbcTemplate") ObjectProvider<JdbcTemplate> apiJdbcTemplateProvider,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        String store = normalize(promptProperties.getStore());

        if ("redis".equals(store)) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                return new RedisPromptRepository(redisTemplate);
            }
        }

        JdbcTemplate apiJdbcTemplate = apiJdbcTemplateProvider.getIfAvailable();
        if (apiJdbcTemplate != null) {
            return new JdbcPromptRepository(apiJdbcTemplate);
        }

        return new InMemoryPromptRepository();
    }

    private String normalize(String value) {
        return value == null ? "jdbc" : value.trim().toLowerCase();
    }
}
