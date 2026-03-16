package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

@Data
public class OllamaConnectionInfo {
    private String baseUrl;
    private boolean reachable;
    private String status;
    private String message;
    private Integer runningCount;
    private Integer installedCount;
}
