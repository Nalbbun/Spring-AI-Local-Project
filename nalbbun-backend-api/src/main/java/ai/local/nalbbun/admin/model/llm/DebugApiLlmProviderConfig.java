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
    private String sllmPath;
    private String llmPath;
    private String embeddingPath;
    private String rerankPath;
    private String searchModel;
    private String answerModel;
    private String embeddingModel;
    private String rerankModel;
}
