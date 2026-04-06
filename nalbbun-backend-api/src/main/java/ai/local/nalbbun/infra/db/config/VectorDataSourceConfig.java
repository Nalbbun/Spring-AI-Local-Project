package ai.local.nalbbun.infra.db.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * RAG / pgvector 전용 데이터소스 구성이다.
 * app.vector.datasource.* 가 지정되면 해당 DB를 사용하고,
 * 지정되지 않으면 spring.datasource.* 또는 기존 dataSource bean 을 fallback 으로 사용한다.
 */
@Configuration
@EnableConfigurationProperties(VectorDataSourceProperties.class)
public class VectorDataSourceConfig {

    @Bean(name = "vectorDataSource")
    @Primary
    public DataSource vectorDataSource(
            VectorDataSourceProperties vectorProperties,
            Environment environment,
            @Qualifier("dataSource") ObjectProvider<DataSource> defaultDataSourceProvider
    ) {
        DataSource existing = defaultDataSourceProvider.getIfAvailable();
        if (!hasText(vectorProperties.getUrl()) && existing != null) {
            return existing;
        }

        String url = firstText(vectorProperties.getUrl(), environment.getProperty("spring.datasource.url"));
        if (!hasText(url)) {
            if (existing == null) {
                throw new IllegalStateException("vectorDataSource를 구성할 수 없습니다. app.vector.datasource.url 또는 spring.datasource.url 을 확인하세요.");
            }
            return existing;
        }

        String username = firstText(vectorProperties.getUsername(), environment.getProperty("spring.datasource.username"));
        String password = vectorProperties.getPassword() != null
                ? vectorProperties.getPassword()
                : environment.getProperty("spring.datasource.password");
        String driverClassName = firstText(vectorProperties.getDriverClassName(), environment.getProperty("spring.datasource.driver-class-name"));

        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        if (hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        return builder.url(url).username(username).password(password).build();
    }

    @Bean(name = "vectorJdbcTemplate")
    @Primary
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
