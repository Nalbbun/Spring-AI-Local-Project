package ai.local.nalbbun.infra.db.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * 애플리케이션 CRUD 전용 데이터소스 구성이다.
 * app.api.datasource.* 가 지정되면 해당 DB를 사용하고,
 * 지정되지 않으면 spring.datasource.* 값을 fallback 으로 사용한다.
 *
 * 메모리 저장소 타입과 무관하게 API 키/프롬프트 CRUD 는 이 API DB 를 기준으로 동작한다.
 */
@Configuration
@EnableConfigurationProperties(ApiDataSourceProperties.class)
@Conditional(ApiDataSourceAvailableCondition.class)
public class ApiDataSourceConfig {

    @Bean(name = "apiDataSource")
    public DataSource apiDataSource(ApiDataSourceProperties apiProperties, Environment environment) {
        String url = firstText(apiProperties.getUrl(), environment.getProperty("spring.datasource.url"));
        String username = firstText(apiProperties.getUsername(), environment.getProperty("spring.datasource.username"));
        String password = apiProperties.getPassword() != null
                ? apiProperties.getPassword()
                : environment.getProperty("spring.datasource.password");
        String driverClassName = firstText(apiProperties.getDriverClassName(), environment.getProperty("spring.datasource.driver-class-name"));

        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        if (hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        return builder.url(url).username(username).password(password).build();
    }

    @Bean(name = "apiJdbcTemplate")
    public JdbcTemplate apiJdbcTemplate(@Qualifier("apiDataSource") DataSource apiDataSource) {
        return new JdbcTemplate(apiDataSource);
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
