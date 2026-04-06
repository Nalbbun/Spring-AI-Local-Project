package ai.local.nalbbun.infra.db.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * API CRUD 전용 데이터소스를 만들 수 있는지 판별한다.
 * app.api.datasource.url 또는 spring.datasource.url 중 하나라도 있으면 활성화된다.
 */
public class ApiDataSourceAvailableCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("app.api.datasource.url"))
                || StringUtils.hasText(env.getProperty("spring.datasource.url"));
    }
}
