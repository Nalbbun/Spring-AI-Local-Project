package ai.local.nalbbun.admin.model.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DebugApiLlmConnectionInfo {
    private String provider;
    private String baseUrl;
    private boolean reachable;
    private String status;
    private String message;
    private String defaultModel;
    private int modelCount;
    private boolean keyResolved;
    private String keyProvider;
    private String healthCheckPath;
    private String healthCheckMethod;
    private String modelsPath;
    private String modelsMethod;
    private boolean healthCheckOk;
    private boolean modelsCheckOk;
    private String resolvedHealthUrl;
    private String resolvedModelsUrl;
    private List<String> availableModels = new ArrayList<>();
    private String sllmPath;
    private String llmPath;
    private String embeddingPath;
    private String rerankPath;
    private String searchModel;
    private String answerModel;
    private String embeddingModel;
    private String rerankModel;
    private String resolvedSllmUrl;
    private String resolvedLlmUrl;
    private String resolvedEmbeddingUrl;
    private String resolvedRerankUrl;
    private Map<String, String> infoModels = new LinkedHashMap<>();
    private Map<String, String> infoEndpoints = new LinkedHashMap<>();
}
