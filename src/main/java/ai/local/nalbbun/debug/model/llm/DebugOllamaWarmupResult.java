package ai.local.nalbbun.debug.model.llm;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DebugOllamaWarmupResult {
    private String baseUrl;
    private String keepAlive;
    private boolean applied;
    private String message;
    private List<String> requestedModels = new ArrayList<>();
    private List<String> warmedModels = new ArrayList<>();
    private List<String> failedModels = new ArrayList<>();
}
