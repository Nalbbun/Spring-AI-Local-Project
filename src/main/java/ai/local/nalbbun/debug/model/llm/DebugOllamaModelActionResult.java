package ai.local.nalbbun.debug.model.llm;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DebugOllamaModelActionResult {
    private String baseUrl;
    private String model;
    private String action;
    private String keepAlive;
    private boolean success;
    private String message;
    private int runningCount;
    private int installedCount;
    private List<String> runningModels = new ArrayList<>();
}
