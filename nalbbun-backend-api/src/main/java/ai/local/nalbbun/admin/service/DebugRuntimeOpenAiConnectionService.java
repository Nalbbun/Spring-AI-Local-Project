package ai.local.nalbbun.admin.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.runtime.port.RuntimeOpenAiConnectionPort;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService;

@Service
public class DebugRuntimeOpenAiConnectionService implements RuntimeOpenAiConnectionPort {
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final String defaultKeyProvider;
    private final String defaultHealthCheckPath;
    private final String defaultHealthCheckMethod;
    private final String defaultModelsPath;
    private final String defaultModelsMethod;

    private final AtomicReference<String> baseUrl;
    private final AtomicReference<String> model;
    private final AtomicReference<String> keyProvider;
    private final AtomicReference<String> healthCheckPath;
    private final AtomicReference<String> healthCheckMethod;
    private final AtomicReference<String> modelsPath;
    private final AtomicReference<String> modelsMethod;
    private final ApiKeyService apiKeyService;

    public DebugRuntimeOpenAiConnectionService(
            @Value("${app.openai-runtime.base-url:https://api.openai.com}") String defaultBaseUrl,
            @Value("${spring.ai.openai.chat.options.model:gpt-4.1-mini}") String defaultModel,
            @Value("${app.openai-runtime.key-provider:OPENAI}") String defaultKeyProvider,
            @Value("${app.openai-runtime.health-path:/v1/models}") String defaultHealthCheckPath,
            @Value("${app.openai-runtime.health-method:GET}") String defaultHealthCheckMethod,
            @Value("${app.openai-runtime.models-path:/v1/models}") String defaultModelsPath,
            @Value("${app.openai-runtime.models-method:GET}") String defaultModelsMethod,
            ApiKeyService apiKeyService) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.defaultModel = safe(defaultModel);
        this.defaultKeyProvider = normalizeProvider(defaultKeyProvider, "OPENAI");
        this.defaultHealthCheckPath = normalizePath(defaultHealthCheckPath, "/v1/models");
        this.defaultHealthCheckMethod = normalizeMethod(defaultHealthCheckMethod, "GET");
        this.defaultModelsPath = normalizePath(defaultModelsPath, "/v1/models");
        this.defaultModelsMethod = normalizeMethod(defaultModelsMethod, "GET");
        this.baseUrl = new AtomicReference<>(this.defaultBaseUrl);
        this.model = new AtomicReference<>(this.defaultModel);
        this.keyProvider = new AtomicReference<>(this.defaultKeyProvider);
        this.healthCheckPath = new AtomicReference<>(this.defaultHealthCheckPath);
        this.healthCheckMethod = new AtomicReference<>(this.defaultHealthCheckMethod);
        this.modelsPath = new AtomicReference<>(this.defaultModelsPath);
        this.modelsMethod = new AtomicReference<>(this.defaultModelsMethod);
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

    public void update(String requestedBaseUrl, String requestedModel, String requestedKeyProvider,
            String requestedHealthCheckPath, String requestedHealthCheckMethod,
            String requestedModelsPath, String requestedModelsMethod) {
        if (requestedBaseUrl != null) baseUrl.set(normalize(requestedBaseUrl));
        if (requestedModel != null) model.set(safe(requestedModel));
        if (requestedKeyProvider != null) keyProvider.set(normalizeProvider(requestedKeyProvider, defaultKeyProvider));
        if (requestedHealthCheckPath != null) healthCheckPath.set(normalizePath(requestedHealthCheckPath, defaultHealthCheckPath));
        if (requestedHealthCheckMethod != null) healthCheckMethod.set(normalizeMethod(requestedHealthCheckMethod, defaultHealthCheckMethod));
        if (requestedModelsPath != null) modelsPath.set(normalizePath(requestedModelsPath, defaultModelsPath));
        if (requestedModelsMethod != null) modelsMethod.set(normalizeMethod(requestedModelsMethod, defaultModelsMethod));
    }

    public void reset() {
        baseUrl.set(defaultBaseUrl);
        model.set(defaultModel);
        keyProvider.set(defaultKeyProvider);
        healthCheckPath.set(defaultHealthCheckPath);
        healthCheckMethod.set(defaultHealthCheckMethod);
        modelsPath.set(defaultModelsPath);
        modelsMethod.set(defaultModelsMethod);
    }

    private String normalize(String value) {
        String v = safe(value);
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.endsWith("/chat/completions")) v = v.substring(0, v.length() - "/chat/completions".length());
        if (v.endsWith("/v1/models")) v = v.substring(0, v.length() - "/v1/models".length());
        if (v.endsWith("/v1")) v = v.substring(0, v.length() - 3);
        return v;
    }

    private String normalizePath(String value, String fallback) {
        String v = safe(value);
        if (v.isBlank()) v = fallback;
        return v.startsWith("/") ? v : "/" + v;
    }

    private String normalizeMethod(String value, String fallback) {
        String v = safe(value).toUpperCase();
        if (!"GET".equals(v) && !"POST".equals(v)) return fallback;
        return v;
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private String normalizeProvider(String provider, String fallback) {
        String value = safe(provider).toUpperCase();
        return value.isBlank() ? fallback : value;
    }
}
