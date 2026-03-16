package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.llm.DebugOllamaConnectionInfo;

@Service
public class DebugRuntimeOllamaConnectionService {

    private final String defaultBaseUrl;
    private final AtomicReference<String> runtimeBaseUrl;

    public DebugRuntimeOllamaConnectionService(
            @Value("${app.ollama.base-url:${spring.ai.ollama.base-url:http://127.0.0.1:11434}}") String defaultBaseUrl
    ) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.runtimeBaseUrl = new AtomicReference<>(this.defaultBaseUrl);
    }

    public String getBaseUrl() {
        return runtimeBaseUrl.get();
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public DebugOllamaConnectionInfo getConnectionInfo() {
        DebugOllamaConnectionInfo info = new DebugOllamaConnectionInfo();
        info.setBaseUrl(getBaseUrl());
        info.setDefaultBaseUrl(defaultBaseUrl);
        info.setRuntimeOverride(!defaultBaseUrl.equals(getBaseUrl()));
        return info;
    }

    public DebugOllamaConnectionInfo update(String baseUrl) {
        runtimeBaseUrl.set(normalize(baseUrl));
        return getConnectionInfo();
    }

    public DebugOllamaConnectionInfo reset() {
        runtimeBaseUrl.set(defaultBaseUrl);
        return getConnectionInfo();
    }

    private String normalize(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "http://127.0.0.1:11434"
                : baseUrl.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
