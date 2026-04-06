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
 * JDBC 메모리 저장소 전용 데이터소스 구성이다.
 * app.memory.jdbc.datasource.* 가 지정되면 해당 DB를 사용하고,
 * 지정되지 않으면 app.api.datasource.* -> spring.datasource.* 순으로 fallback 한다.
 */
@Configuration
@EnableConfigurationProperties(MemoryJdbcDataSourceProperties.class)
@Conditional(MemoryJdbcDataSourceAvailableCondition.class)
public class MemoryJdbcDataSourceConfig {

    @Bean(name = "memoryJdbcDataSource")
    public DataSource memoryJdbcDataSource(MemoryJdbcDataSourceProperties memoryProperties, Environment environment) {
        String url = firstText(
                memoryProperties.getUrl(),
                environment.getProperty("app.api.datasource.url"),
                environment.getProperty("spring.datasource.url")
        );
        String username = firstText(
                memoryProperties.getUsername(),
                environment.getProperty("app.api.datasource.username"),
                environment.getProperty("spring.datasource.username")
        );
        String password = memoryProperties.getPassword() != null
                ? memoryProperties.getPassword()
                : firstText(environment.getProperty("app.api.datasource.password"), environment.getProperty("spring.datasource.password"));
        String driverClassName = firstText(
                memoryProperties.getDriverClassName(),
                environment.getProperty("app.api.datasource.driver-class-name"),
                environment.getProperty("spring.datasource.driver-class-name")
        );

        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        if (hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        return builder.url(url).username(username).password(password).build();
    }

    @Bean(name = "memoryJdbcTemplate")
    public JdbcTemplate memoryJdbcTemplate(@Qualifier("memoryJdbcDataSource") DataSource memoryJdbcDataSource) {
        return new JdbcTemplate(memoryJdbcDataSource);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) return value;
        }
        return null;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
