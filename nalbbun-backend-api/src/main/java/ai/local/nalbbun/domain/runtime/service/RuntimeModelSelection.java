package ai.local.nalbbun.domain.runtime.service;

import ai.local.nalbbun.domain.runtime.model.RuntimeLlmProvider;

/**
 * Runtime Model Selection 타입이다.
 */
public record RuntimeModelSelection(
        RuntimeLlmProvider provider,
        String modelName,
        String baseUrl,
        String keyProvider,
        boolean fallbackApplied,
        String reason
) {
    public boolean ollama() {
        return provider == RuntimeLlmProvider.OLLAMA;
    }

    public boolean apiCompatible() {
        return provider != null && provider.isApiCompatible();
    }

    public String describe() {
        String model = (modelName == null || modelName.isBlank()) ? "default" : modelName;
        String base = (baseUrl == null || baseUrl.isBlank()) ? "default" : baseUrl;
        String providerName = provider == null ? "UNKNOWN" : provider.name();
        String desc = providerName + ':' + model + '@' + base;
        if (keyProvider != null && !keyProvider.isBlank()) {
            desc += " [key=" + keyProvider + ']';
        }
        if (fallbackApplied) {
            desc += " (fallback: " + reason + ')';
        }
        return desc;
    }
}
