package ai.local.nalbbun.admin.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService;

@Service
public class DebugRuntimeVllmConnectionService implements RuntimeVllmConnectionPort {
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final String defaultKeyProvider;
    private final String defaultHealthCheckPath;
    private final String defaultHealthCheckMethod;
    private final String defaultModelsPath;
    private final String defaultModelsMethod;
    private final String defaultSllmPath;
    private final String defaultLlmPath;
    private final String defaultEmbeddingPath;
    private final String defaultRerankPath;
    private final String defaultSearchModel;
    private final String defaultAnswerModel;
    private final String defaultEmbeddingModel;
    private final String defaultRerankModel;

    private final AtomicReference<String> baseUrl;
    private final AtomicReference<String> model;
    private final AtomicReference<String> keyProvider;
    private final AtomicReference<String> healthCheckPath;
    private final AtomicReference<String> healthCheckMethod;
    private final AtomicReference<String> modelsPath;
    private final AtomicReference<String> modelsMethod;
    private final AtomicReference<String> sllmPath;
    private final AtomicReference<String> llmPath;
    private final AtomicReference<String> embeddingPath;
    private final AtomicReference<String> rerankPath;
    private final AtomicReference<String> searchModel;
    private final AtomicReference<String> answerModel;
    private final AtomicReference<String> embeddingModel;
    private final AtomicReference<String> rerankModel;
    private final ApiKeyService apiKeyService;

    public DebugRuntimeVllmConnectionService(
            @Value("${app.vllm.base-url:http://127.0.0.1:8000}") String defaultBaseUrl,
            @Value("${app.vllm.default-model:}") String defaultModel,
            @Value("${app.vllm.key-provider:VLLM}") String defaultKeyProvider,
            @Value("${app.vllm.health-path:/api/info}") String defaultHealthCheckPath,
            @Value("${app.vllm.health-method:GET}") String defaultHealthCheckMethod,
            @Value("${app.vllm.models-path:/v1/models}") String defaultModelsPath,
            @Value("${app.vllm.models-method:GET}") String defaultModelsMethod,
            @Value("${app.vllm.sllm-path:/sllm}") String defaultSllmPath,
            @Value("${app.vllm.llm-path:/llm}") String defaultLlmPath,
            @Value("${app.vllm.embedding-path:/embedding/api}") String defaultEmbeddingPath,
            @Value("${app.vllm.rerank-path:/rerank}") String defaultRerankPath,
            @Value("${app.vllm.search-model:exaone-3.5-2.4b-it}") String defaultSearchModel,
            @Value("${app.vllm.answer-model:exaone-3.5-32b-it}") String defaultAnswerModel,
            @Value("${app.vllm.embedding-model:BAAI/bge-m3}") String defaultEmbeddingModel,
            @Value("${app.vllm.rerank-model:BAAI/bge-reranker-v2-m3}") String defaultRerankModel,
            ApiKeyService apiKeyService) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.defaultModel = safe(defaultModel);
        this.defaultKeyProvider = normalizeProvider(defaultKeyProvider, "VLLM");
        this.defaultHealthCheckPath = normalizePath(defaultHealthCheckPath, "/api/info");
        this.defaultHealthCheckMethod = normalizeMethod(defaultHealthCheckMethod, "GET");
        this.defaultModelsPath = normalizePath(defaultModelsPath, "/v1/models");
        this.defaultModelsMethod = normalizeMethod(defaultModelsMethod, "GET");
        this.defaultSllmPath = normalizePath(defaultSllmPath, "/sllm");
        this.defaultLlmPath = normalizePath(defaultLlmPath, "/llm");
        this.defaultEmbeddingPath = normalizePath(defaultEmbeddingPath, "/embedding/api");
        this.defaultRerankPath = normalizePath(defaultRerankPath, "/rerank");
        this.defaultSearchModel = safe(defaultSearchModel);
        this.defaultAnswerModel = safe(defaultAnswerModel);
        this.defaultEmbeddingModel = safe(defaultEmbeddingModel);
        this.defaultRerankModel = safe(defaultRerankModel);
        this.baseUrl = new AtomicReference<>(this.defaultBaseUrl);
        this.model = new AtomicReference<>(this.defaultModel);
        this.keyProvider = new AtomicReference<>(this.defaultKeyProvider);
        this.healthCheckPath = new AtomicReference<>(this.defaultHealthCheckPath);
        this.healthCheckMethod = new AtomicReference<>(this.defaultHealthCheckMethod);
        this.modelsPath = new AtomicReference<>(this.defaultModelsPath);
        this.modelsMethod = new AtomicReference<>(this.defaultModelsMethod);
        this.sllmPath = new AtomicReference<>(this.defaultSllmPath);
        this.llmPath = new AtomicReference<>(this.defaultLlmPath);
        this.embeddingPath = new AtomicReference<>(this.defaultEmbeddingPath);
        this.rerankPath = new AtomicReference<>(this.defaultRerankPath);
        this.searchModel = new AtomicReference<>(this.defaultSearchModel);
        this.answerModel = new AtomicReference<>(this.defaultAnswerModel);
        this.embeddingModel = new AtomicReference<>(this.defaultEmbeddingModel);
        this.rerankModel = new AtomicReference<>(this.defaultRerankModel);
        this.apiKeyService = apiKeyService;
    }

    public String getBaseUrl() { return baseUrl.get(); }
    public String getDefaultModel() { return defaultModel; }
    public String getConfiguredOrDefaultModel() { return safe(model.get()).isBlank() ? defaultModel : safe(model.get()); }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public String getKeyProvider() { return keyProvider.get(); }
    public String getResolvedApiKey() { return apiKeyService.findActivePlainKey(getKeyProvider()).orElse(""); }
    public String getHealthCheckPath() { return healthCheckPath.get(); }
    public String getHealthCheckMethod() { return healthCheckMethod.get(); }
    public String getModelsPath() { return modelsPath.get(); }
    public String getModelsMethod() { return modelsMethod.get(); }
    public String getSllmPath() { return sllmPath.get(); }
    public String getLlmPath() { return llmPath.get(); }
    public String getEmbeddingPath() { return embeddingPath.get(); }
    public String getRerankPath() { return rerankPath.get(); }
    public String getSearchModel() { return safe(searchModel.get()).isBlank() ? defaultSearchModel : safe(searchModel.get()); }
    public String getAnswerModel() { return safe(answerModel.get()).isBlank() ? defaultAnswerModel : safe(answerModel.get()); }
    public String getEmbeddingModel() { return safe(embeddingModel.get()).isBlank() ? defaultEmbeddingModel : safe(embeddingModel.get()); }
    public String getRerankModel() { return safe(rerankModel.get()).isBlank() ? defaultRerankModel : safe(rerankModel.get()); }

    public void update(String requestedBaseUrl, String requestedModel, String requestedKeyProvider,
            String requestedHealthCheckPath, String requestedHealthCheckMethod,
            String requestedModelsPath, String requestedModelsMethod,
            String requestedSllmPath, String requestedLlmPath,
            String requestedEmbeddingPath, String requestedRerankPath,
            String requestedSearchModel, String requestedAnswerModel,
            String requestedEmbeddingModel, String requestedRerankModel) {
        if (requestedBaseUrl != null) baseUrl.set(normalize(requestedBaseUrl));
        if (requestedModel != null) model.set(safe(requestedModel));
        if (requestedKeyProvider != null) keyProvider.set(normalizeProvider(requestedKeyProvider, defaultKeyProvider));
        if (requestedHealthCheckPath != null) healthCheckPath.set(normalizePath(requestedHealthCheckPath, defaultHealthCheckPath));
        if (requestedHealthCheckMethod != null) healthCheckMethod.set(normalizeMethod(requestedHealthCheckMethod, defaultHealthCheckMethod));
        if (requestedModelsPath != null) modelsPath.set(normalizePath(requestedModelsPath, defaultModelsPath));
        if (requestedModelsMethod != null) modelsMethod.set(normalizeMethod(requestedModelsMethod, defaultModelsMethod));
        if (requestedSllmPath != null) sllmPath.set(normalizePath(requestedSllmPath, defaultSllmPath));
        if (requestedLlmPath != null) llmPath.set(normalizePath(requestedLlmPath, defaultLlmPath));
        if (requestedEmbeddingPath != null) embeddingPath.set(normalizePath(requestedEmbeddingPath, defaultEmbeddingPath));
        if (requestedRerankPath != null) rerankPath.set(normalizePath(requestedRerankPath, defaultRerankPath));
        if (requestedSearchModel != null) searchModel.set(safe(requestedSearchModel));
        if (requestedAnswerModel != null) answerModel.set(safe(requestedAnswerModel));
        if (requestedEmbeddingModel != null) embeddingModel.set(safe(requestedEmbeddingModel));
        if (requestedRerankModel != null) rerankModel.set(safe(requestedRerankModel));
    }

    public void reset() {
        baseUrl.set(defaultBaseUrl); model.set(defaultModel); keyProvider.set(defaultKeyProvider);
        healthCheckPath.set(defaultHealthCheckPath); healthCheckMethod.set(defaultHealthCheckMethod);
        modelsPath.set(defaultModelsPath); modelsMethod.set(defaultModelsMethod);
        sllmPath.set(defaultSllmPath); llmPath.set(defaultLlmPath); embeddingPath.set(defaultEmbeddingPath); rerankPath.set(defaultRerankPath);
        searchModel.set(defaultSearchModel); answerModel.set(defaultAnswerModel); embeddingModel.set(defaultEmbeddingModel); rerankModel.set(defaultRerankModel);
    }

    private String normalize(String value) {
        String v = safe(value);
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.endsWith("/api/info")) v = v.substring(0, v.length() - "/api/info".length());
        if (v.endsWith("/v1/models")) v = v.substring(0, v.length() - "/v1/models".length());
        if (v.endsWith("/v1/chat/completions")) v = v.substring(0, v.length() - "/v1/chat/completions".length());
        if (v.endsWith("/v1/embeddings")) v = v.substring(0, v.length() - "/v1/embeddings".length());
        return v;
    }
    private String normalizePath(String value, String fallback) { String v = safe(value); if (v.isBlank()) v = fallback; return v.startsWith("/") ? v : "/" + v; }
    private String normalizeMethod(String value, String fallback) { String v = safe(value).toUpperCase(); return (!"GET".equals(v) && !"POST".equals(v)) ? fallback : v; }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private String normalizeProvider(String provider, String fallback) { String value = safe(provider).toUpperCase(); return value.isBlank() ? fallback : value; }
}
