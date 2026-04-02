package ai.local.nalbbun.domain.rag.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.admin.model.llm.OllamaModelSource;
import ai.local.nalbbun.admin.service.OllamaModelDiscoveryService;
import ai.local.nalbbun.domain.runtime.port.RuntimeOpenAiConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingConfigService {
    private final RuntimeOllamaVectorStoreFactory vectorStoreFactory;
    private final OllamaModelDiscoveryService modelDiscoveryService;
    private final RuntimeVllmConnectionPort vllmConnectionPort;
    private final RuntimeOpenAiConnectionPort openAiConnectionPort;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String defaultModel;
    @Value("${spring.ai.ollama.embedding.options.keep-alive:300s}")
    private String defaultKeepAlive;
    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int defaultDimensions;

    private final AtomicReference<String> currentModel = new AtomicReference<>();
    private final AtomicReference<String> currentKeepAlive = new AtomicReference<>();
    private final AtomicReference<Integer> currentDimensions = new AtomicReference<>();

    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model", getModel()); config.put("keepAlive", getKeepAlive()); config.put("dimensions", getDimensions());
        config.put("defaultModel", defaultModel); config.put("defaultKeepAlive", defaultKeepAlive); config.put("defaultDimensions", defaultDimensions);
        config.put("availableProviders", List.of("OLLAMA", "VLLM", "OPENAI"));
        return config;
    }

    public Map<String, Object> applyConfig(String model, String keepAlive, Integer dimensions) {
        if (model != null && !model.isBlank()) currentModel.set(model.trim());
        if (keepAlive != null && !keepAlive.isBlank()) currentKeepAlive.set(normalizeKeepAlive(keepAlive.trim()));
        if (dimensions != null && dimensions > 0) currentDimensions.set(dimensions);
        vectorStoreFactory.setEmbeddingModel(getModel()); vectorStoreFactory.setEmbeddingKeepAlive(getKeepAlive()); vectorStoreFactory.setDimensions(getDimensions());
        log.info("Embedding config applied. model={}, keepAlive={}, dimensions={}", getModel(), getKeepAlive(), getDimensions());
        return getCurrentConfig();
    }

    public Map<String, Object> resetConfig() {
        currentModel.set(null); currentKeepAlive.set(null); currentDimensions.set(null);
        vectorStoreFactory.setEmbeddingModel(defaultModel); vectorStoreFactory.setEmbeddingKeepAlive(defaultKeepAlive); vectorStoreFactory.setDimensions(defaultDimensions);
        log.info("Embedding config reset to defaults.");
        return getCurrentConfig();
    }

    public List<String> listAvailableModels() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        try {
            modelDiscoveryService.getModels(OllamaModelSource.INSTALLED).stream().map(m -> m.getName() != null ? m.getName() : m.getModel())
                    .filter(n -> n != null && !n.isBlank()).map(n -> "OLLAMA::" + n).forEach(result::add);
        } catch (Exception e) {
            log.warn("Ollama 임베딩 모델 목록 조회 실패: {}", e.getMessage());
        }
        if (vllmConnectionPort.getEmbeddingModel() != null && !vllmConnectionPort.getEmbeddingModel().isBlank()) result.add("VLLM::" + vllmConnectionPort.getEmbeddingModel());
        result.add("OPENAI::text-embedding-3-small");
        return result.stream().toList();
    }

    public String getModel() { String v = currentModel.get(); return (v != null) ? v : defaultModel; }
    public String getKeepAlive() { String v = currentKeepAlive.get(); return (v != null) ? v : defaultKeepAlive; }
    public int getDimensions() { Integer v = currentDimensions.get(); return (v != null) ? v : defaultDimensions; }
    private String normalizeKeepAlive(String value) { if (value == null || value.isBlank()) return "300s"; if (Character.isDigit(value.charAt(value.length()-1))) return value + "s"; return value; }
}
