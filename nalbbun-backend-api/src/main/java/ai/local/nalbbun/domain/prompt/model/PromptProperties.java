package ai.local.nalbbun.domain.prompt.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 저장소 설정.
 * app.prompt.store: jdbc (기본값) | redis | in-memory
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.prompt")
public class PromptProperties {

    /** 저장소 타입: in-memory | jdbc | redis */
    private String store = "jdbc";
}
