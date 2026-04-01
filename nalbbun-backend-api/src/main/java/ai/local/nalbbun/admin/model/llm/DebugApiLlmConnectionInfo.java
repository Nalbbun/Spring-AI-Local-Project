package ai.local.nalbbun.admin.model.llm;

import java.util.ArrayList;
import java.util.List;

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
}
