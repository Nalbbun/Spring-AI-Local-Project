package ai.local.nalbbun.infra.config;

import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import ai.local.nalbbun.infra.db.apikey.jdbc.JdbcApiKeyRepository;
import ai.local.nalbbun.infra.db.apikey.noop.NoopApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * API 키 저장소 빈 구성이다.
 *
 * <p>주의: 기존 @ConditionalOnBean(name = "apiJdbcTemplate") 방식은
 * 설정 클래스 처리 순서에 따라 apiJdbcTemplate 빈 정의가 늦게 등록되면
 * 잘못하여 no-op 저장소가 선택될 수 있었다.</p>
 *
 * <p>이 구현은 빈 생성 시점에 ObjectProvider 로 실제 API JDBC 템플릿 존재 여부를 확인하여
 * 메모리 타입과 무관하게 API DB 저장소를 우선 사용하도록 고정한다.</p>
 */
@Slf4j
@Configuration
public class ApiKeyRepositoryConfig {

    @Bean
    public ApiKeyRepository apiKeyRepository(@Qualifier("apiJdbcTemplate") ObjectProvider<JdbcTemplate> apiJdbcTemplateProvider) {
        JdbcTemplate apiJdbcTemplate = apiJdbcTemplateProvider.getIfAvailable();
        if (apiJdbcTemplate != null) {
            log.info("API 키 저장소: JDBC(apiJdbcTemplate) 사용");
            return new JdbcApiKeyRepository(apiJdbcTemplate);
        }
        log.warn("API 키 저장소: apiJdbcTemplate 미생성으로 no-op 저장소 사용");
        return new NoopApiKeyRepository();
    }
}
