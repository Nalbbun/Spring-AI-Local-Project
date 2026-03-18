package ai.local.nalbbun.rag.service;

import ai.local.nalbbun.internal.service.OllamaModelDiscoveryService;
import ai.local.nalbbun.internal.model.llm.OllamaModelSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 임베딩 모델 런타임 설정 서비스.
 * RAG 인제스트·검색 시 사용할 임베딩 모델을 런타임으로 변경합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingConfigService {

    private final RuntimeOllamaVectorStoreFactory vectorStoreFactory;
    private final OllamaModelDiscoveryService modelDiscoveryService;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String defaultModel;

    @Value("${spring.ai.ollama.embedding.options.keep-alive:300s}")
    private String defaultKeepAlive;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int defaultDimensions;

    // 런타임 상태 (AtomicReference로 thread-safe 관리)
    private final AtomicReference<String> currentModel      = new AtomicReference<>();
    private final AtomicReference<String> currentKeepAlive  = new AtomicReference<>();
    private final AtomicReference<Integer> currentDimensions = new AtomicReference<>();

    /** 현재 적용 중인 설정 조회 */
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model",         getModel());
        config.put("keepAlive",     getKeepAlive());
        config.put("dimensions",    getDimensions());
        config.put("defaultModel",  defaultModel);
        config.put("defaultKeepAlive", defaultKeepAlive);
        config.put("defaultDimensions", defaultDimensions);
        return config;
    }

    /** 설정 변경 및 VectorStoreFactory에 반영 */
    public Map<String, Object> applyConfig(String model, String keepAlive, Integer dimensions) {
        if (model != null && !model.isBlank()) {
            currentModel.set(model.trim());
        }
        if (keepAlive != null && !keepAlive.isBlank()) {
            currentKeepAlive.set(normalizeKeepAlive(keepAlive.trim()));
        }
        if (dimensions != null && dimensions > 0) {
            currentDimensions.set(dimensions);
        }

        // VectorStoreFactory에 반영
        vectorStoreFactory.setEmbeddingModel(getModel());
        vectorStoreFactory.setEmbeddingKeepAlive(getKeepAlive());
        vectorStoreFactory.setDimensions(getDimensions());

        log.info("Embedding config applied. model={}, keepAlive={}, dimensions={}",
                getModel(), getKeepAlive(), getDimensions());
        return getCurrentConfig();
    }

    /** 기본값으로 초기화 */
    public Map<String, Object> resetConfig() {
        currentModel.set(null);
        currentKeepAlive.set(null);
        currentDimensions.set(null);
        vectorStoreFactory.setEmbeddingModel(defaultModel);
        vectorStoreFactory.setEmbeddingKeepAlive(defaultKeepAlive);
        vectorStoreFactory.setDimensions(defaultDimensions);
        log.info("Embedding config reset to defaults.");
        return getCurrentConfig();
    }

    /** Ollama에서 임베딩 가능 모델 목록 조회 (INSTALLED 전체) */
    public List<String> listAvailableModels() {
        try {
            return modelDiscoveryService.getModels(OllamaModelSource.INSTALLED)
                    .stream()
                    .map(m -> m.getName() != null ? m.getName() : m.getModel())
                    .filter(n -> n != null && !n.isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("Ollama 임베딩 모델 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ── 내부 접근자 ──────────────────────────────────
    public String getModel() {
        String v = currentModel.get();
        return (v != null) ? v : defaultModel;
    }

    public String getKeepAlive() {
        String v = currentKeepAlive.get();
        return (v != null) ? v : defaultKeepAlive;
    }

    public int getDimensions() {
        Integer v = currentDimensions.get();
        return (v != null) ? v : defaultDimensions;
    }

    private String normalizeKeepAlive(String value) {
        if (value == null || value.isBlank()) return "300s";
        // 숫자만 있으면 's' 단위 추가
        if (Character.isDigit(value.charAt(value.length() - 1))) return value + "s";
        return value;
    }
}
