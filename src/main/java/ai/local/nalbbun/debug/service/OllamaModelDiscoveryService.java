package ai.local.nalbbun.debug.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import ai.local.nalbbun.debug.model.llm.OllamaModelInfo;
import ai.local.nalbbun.debug.model.llm.OllamaModelSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OllamaModelDiscoveryService {

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OllamaModelDiscoveryService(
            @Value("${app.ollama.base-url:http://192.168.1.10:11434}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
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
                existing.setState("RUNNING+INSTALLED");
                existing.setDisplayName("[RUNNING+INSTALLED] " + nonBlank(existing.getName(), existing.getModel(), "unknown"));
                if (existing.getSize() == null && model.getSize() != null) {
                    existing.setSize(model.getSize());
                }
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(m -> nonBlank(m.getName(), m.getModel(), "zzzz")))
                .toList();
    }

    private List<OllamaModelInfo> fetchModels(String uri, String state) {
        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = jsonMapper.readTree(body);
            JsonNode models = root.path("models");
            if (!models.isArray()) {
                return List.of();
            }

            List<OllamaModelInfo> result = new ArrayList<>();
            for (JsonNode node : models) {
                String name = nonBlank(node.path("name").asText(null), node.path("model").asText(null), "unknown");
                String model = nonBlank(node.path("model").asText(null), name, "unknown");
                Long size = node.hasNonNull("size") ? node.path("size").asLong() : null;
                OllamaModelInfo info = new OllamaModelInfo();
                info.setName(name);
                info.setModel(model);
                info.setSize(size);
                info.setState(state);
                info.setDisplayName("[" + state + "] " + name);
                result.add(info);
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String keyOf(OllamaModelInfo model) {
        return nonBlank(model.getName(), model.getModel(), "unknown");
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
