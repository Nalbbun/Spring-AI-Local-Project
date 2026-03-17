package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DebugRuntimeOllamaConnectionService {

    private final String defaultBaseUrl;
    private final AtomicReference<String> baseUrl;

    public DebugRuntimeOllamaConnectionService(
            @Value("${spring.ai.ollama.base-url:http://127.0.0.1:11434}") String defaultBaseUrl
    ) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.baseUrl = new AtomicReference<>(this.defaultBaseUrl);
    }

    public String getBaseUrl() {
        return baseUrl.get();
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String update(String requestedBaseUrl) {
        String normalized = normalize(requestedBaseUrl);
        if (normalized.isBlank()) {
            normalized = defaultBaseUrl;
        }
        baseUrl.set(normalized);
        return normalized;
    }

    public String reset() {
        baseUrl.set(defaultBaseUrl);
        return defaultBaseUrl;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
