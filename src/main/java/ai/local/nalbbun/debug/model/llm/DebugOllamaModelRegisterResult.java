package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class DebugOllamaModelRegisterResult {

    private String baseUrl;
    private String model;
    private String keepAlive;

    private boolean requestedPull;
    private boolean requestedWarmup;

    private boolean installAttempted;
    private boolean installSuccess;
    private boolean warmupAttempted;
    private boolean warmupSuccess;

    private Integer installedCount;
    private Integer runningCount;

    private String status;
    private String message;
}
