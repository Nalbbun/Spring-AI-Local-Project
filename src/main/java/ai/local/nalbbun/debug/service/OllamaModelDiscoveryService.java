package ai.local.nalbbun.debug.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.DebugOllamaWarmupResult;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OllamaModelDiscoveryService {

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final DebugRuntimeModelConfigService modelConfigService;
    private final OllamaRuntimeKeepAliveService keepAliveService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OllamaModelDiscoveryService(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            DebugRuntimeModelConfigService modelConfigService,
            OllamaRuntimeKeepAliveService keepAliveService
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.modelConfigService = modelConfigService;
        this.keepAliveService = keepAliveService;
    }

    public DebugOllamaConnectionInfo getDebugConnectionInfo() {
        DebugOllamaConnectionInfo info = ollamaConnectionService.getConnectionInfo();
        info.setResidentModels(modelConfigService.getResidentModels());
        info.setResidentKeepAlive(modelConfigService.getResidentKeepAlive());
        info.setAutoWarmupWhenNoRunningModels(modelConfigService.isAutoWarmupWhenNoRunningModels());

        try {
            List<OllamaModelInfo> installed = getInstalledModels();
            List<OllamaModelInfo> running = getRunningModels();
            info.setReachable(true);
            info.setStatus("OK");
            info.setRunningCount(running.size());
            info.setInstalledCount(installed.size());
            if (running.isEmpty()) {
                if (modelConfigService.isAutoWarmupWhenNoRunningModels() && !modelConfigService.getResidentModelList().isEmpty()) {
                    info.setMessage("connected, resident warmup attempted but no running model is visible yet");
                } else {
                    info.setMessage("connected, no running model");
                }
            } else {
                info.setMessage("connected");
            }
        } catch (Exception e) {
            info.setReachable(false);
            info.setStatus("ERROR");
            info.setMessage(e.getMessage());
            info.setRunningCount(0);
            info.setInstalledCount(0);
        }
        return info;
    }

    public List<OllamaModelInfo> getRunningModels() {
        return fetchRunningModelsWithWarmup();
    }

    public List<OllamaModelInfo> getInstalledModels() {
        return fetchModelsStrict("/api/tags", "INSTALLED");
    }

    public List<OllamaModelInfo> getModels(OllamaModelSource source) {
        if (source == OllamaModelSource.RUNNING) {
            return getRunningModels();
        }
        if (source == OllamaModelSource.INSTALLED) {
            return getInstalledModels();
        }

        Map<String, OllamaModelInfo> merged = new LinkedHashMap<>();
        for (OllamaModelInfo model : getInstalledModels()) {
            merged.put(keyOf(model), model);
        }
        for (OllamaModelInfo model : getRunningModels()) {
            String key = keyOf(model);
            OllamaModelInfo existing = merged.get(key);
            if (existing == null) {
                merged.put(key, model);
            } else {
                existing.setState("RUNNING+INSTALLED");
                if (model.getSize() != null) {
                    existing.setSize(model.getSize());
                }
                if (model.getModifiedAt() != null) {
                    existing.setModifiedAt(model.getModifiedAt());
                }
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(OllamaModelInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<OllamaModelInfo> fetchRunningModelsWithWarmup() {
        List<OllamaModelInfo> running = fetchModelsStrict("/api/ps", "RUNNING");
        if (!running.isEmpty()) {
            return running;
        }
        if (!modelConfigService.isAutoWarmupWhenNoRunningModels()) {
            return running;
        }
        if (modelConfigService.getResidentModelList().isEmpty()) {
            return running;
        }

        DebugOllamaWarmupResult warmup = keepAliveService.warmupConfiguredResidentModels();
        if (!warmup.isApplied()) {
            return running;
        }
        return fetchModelsStrict("/api/ps", "RUNNING");
    }

    private List<OllamaModelInfo> fetchModelsStrict(String uri, String state) {
        try {
            String body = restClient().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(body == null ? "{}" : body);
            JsonNode models = root.path("models");
            if (!models.isArray()) {
                return List.of();
            }

            List<OllamaModelInfo> result = new ArrayList<>();
            for (JsonNode node : models) {
                String name = text(node, "name");
                if (name == null || name.isBlank()) {
                    continue;
                }
                OllamaModelInfo info = new OllamaModelInfo();
                info.setName(name);
                info.setModel(text(node, "model"));
                info.setDisplayName(text(node, "display_name"));
                info.setState(state);
                info.setModifiedAt(text(node, "modified_at"));
                info.setSize(node.path("size").isNumber() ? node.path("size").asLong() : null);
                result.add(info);
            }
            result.sort(Comparator.comparing(OllamaModelInfo::getName, String.CASE_INSENSITIVE_ORDER));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Ollama 연결 실패: " + e.getMessage(), e);
        }
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .build();
    }

    private String keyOf(OllamaModelInfo model) {
        return model.getName() == null ? "" : model.getName().trim().toLowerCase();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asText();
    }
}
