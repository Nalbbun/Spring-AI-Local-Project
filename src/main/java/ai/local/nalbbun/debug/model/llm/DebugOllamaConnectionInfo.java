package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class DebugOllamaConnectionInfo {

    private String baseUrl;
    private String defaultBaseUrl;
    private boolean runtimeOverride;

    private boolean reachable;
    private String status;
    private String message;
    private Integer runningCount;
    private Integer installedCount;
}
