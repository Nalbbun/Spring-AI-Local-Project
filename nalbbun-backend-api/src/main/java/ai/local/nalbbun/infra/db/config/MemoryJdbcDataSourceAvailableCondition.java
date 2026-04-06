package ai.local.nalbbun.infra.db.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * JDBC 메모리 DB 연결 정보 존재 여부를 판별한다.
 */
public class MemoryJdbcDataSourceAvailableCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String memoryUrl = context.getEnvironment().getProperty("app.memory.jdbc.datasource.url");
        String apiUrl = context.getEnvironment().getProperty("app.api.datasource.url");
        String springUrl = context.getEnvironment().getProperty("spring.datasource.url");
        return StringUtils.hasText(memoryUrl) || StringUtils.hasText(apiUrl) || StringUtils.hasText(springUrl);
    }
}
