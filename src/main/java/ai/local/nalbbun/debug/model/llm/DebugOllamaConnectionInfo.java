package ai.local.nalbbun.debug.model.llm;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DebugOllamaConnectionInfo {
    private String baseUrl;
    private boolean reachable;
    private String status;
    private String message;
    private int runningCount;
    private int installedCount;
    private List<String> runningModels = new ArrayList<>();
}
