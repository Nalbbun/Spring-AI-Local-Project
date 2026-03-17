package ai.local.nalbbun.debug.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OllamaModelDiscoveryService {

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public OllamaModelDiscoveryService(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            @Value("${app.ollama.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.ollama.request-timeout-ms:5000}") int readTimeoutMs
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.connectTimeoutMs = Math.max(1000, connectTimeoutMs);
        this.readTimeoutMs = Math.max(this.connectTimeoutMs, readTimeoutMs);
    }

    public DebugOllamaConnectionInfo getDebugConnectionInfo() {
        DebugOllamaConnectionInfo info = baseInfo();
        try {
            restClient().get()
                    .uri("/api/version")
                    .retrieve()
                    .body(String.class);

            List<OllamaModelInfo> running = getRunningModels();
            List<OllamaModelInfo> installed = getInstalledModels();

            info.setReachable(true);
            info.setStatus("OK");
            info.setMessage("connected");
            info.setRunningCount(running.size());
            info.setInstalledCount(installed.size());
        } catch (Exception e) {
            info.setReachable(false);
            info.setStatus("ERROR");
            info.setMessage(rootMessage(e));
            info.setRunningCount(0);
            info.setInstalledCount(0);
        }
        return info;
    }

    public List<OllamaModelInfo> getRunningModels() {
        return fetchModels("/api/ps", "RUNNING");
    }

    public List<OllamaModelInfo> getInstalledModels() {
        return fetchModels("/api/tags", "INSTALLED");
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
                existing.setState("RUNNING");
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

    private List<OllamaModelInfo> fetchModels(String uri, String state) {
        String body = restClient().get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        JsonNode root;
        try {
            root = jsonMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            throw new IllegalStateException("Ollama response parse failed: " + rootMessage(e), e);
        }

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
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private DebugOllamaConnectionInfo baseInfo() {
        DebugOllamaConnectionInfo info = new DebugOllamaConnectionInfo();
        info.setBaseUrl(ollamaConnectionService.getBaseUrl());
        info.setDefaultBaseUrl(ollamaConnectionService.getDefaultBaseUrl());
        info.setRuntimeOverride(!ollamaConnectionService.getDefaultBaseUrl().equals(ollamaConnectionService.getBaseUrl()));
        return info;
    }

    private String keyOf(OllamaModelInfo model) {
        return model.getName() == null ? "" : model.getName().trim().toLowerCase();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asText();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
