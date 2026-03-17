package ai.local.nalbbun.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnExpression(
        "'${app.memory.store:in-memory}' == 'jdbc' || ('${app.rag.enabled:false}' == 'true' && '${app.rag.vector-store:pgvector}' == 'pgvector')"
)
/**
 * JdbcMemoryDataSourceConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: jdbc memory data source config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public class JdbcMemoryDataSourceConfig {

    /**
     * dataSourceProperties 기능을 수행한다.
     * @return DataSourceProperties 타입의 처리 결과
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * dataSource 기능을 수행한다.
     *
     * @param dataSourceProperties dataSourceProperties 값
     * @return DataSource 타입의 처리 결과
     */
    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    /**
     * jdbcTemplate 기능을 수행한다.
     *
     * @param dataSource dataSource 값
     * @return JdbcTemplate 타입의 처리 결과
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}