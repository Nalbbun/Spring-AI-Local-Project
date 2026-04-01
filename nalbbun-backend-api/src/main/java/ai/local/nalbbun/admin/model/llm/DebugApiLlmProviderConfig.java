package ai.local.nalbbun.admin.model.llm;

import lombok.Data;

@Data
public class DebugApiLlmProviderConfig {
    private String baseUrl;
    private String defaultModel;
    private String keyProvider;
    private String healthCheckPath;
    private String healthCheckMethod;
    private String modelsPath;
    private String modelsMethod;
}
