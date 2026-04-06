package ai.local.nalbbun.domain.runtime.port;

public interface RuntimeVllmConnectionPort {
    String getBaseUrl();
    String getConfiguredOrDefaultModel();
    String getKeyProvider();
    String getResolvedApiKey();
    String getHealthCheckPath();
    String getHealthCheckMethod();
    String getModelsPath();
    String getModelsMethod();
    String getSllmPath();
    String getLlmPath();
    String getEmbeddingPath();
    String getRerankPath();
    String getSearchModel();
    String getAnswerModel();
    String getEmbeddingModel();
    String getRerankModel();

    default String getSllmBaseUrl() { return toOpenAiBase(join(getBaseUrl(), getSllmPath())); }
    default String getLlmBaseUrl() { return toOpenAiBase(join(getBaseUrl(), getLlmPath())); }
    default String getEmbeddingBaseUrl() { return toEmbeddingBase(join(getBaseUrl(), getEmbeddingPath())); }
    default String getRerankBaseUrl() { return toRerankBase(join(getBaseUrl(), getRerankPath())); }

    default String getResolvedSllmRequestUrl() { return toChatCompletionUrl(join(getBaseUrl(), getSllmPath())); }
    default String getResolvedLlmRequestUrl() { return toChatCompletionUrl(join(getBaseUrl(), getLlmPath())); }
    default String getResolvedEmbeddingRequestUrl() { return toEmbeddingRequestUrl(join(getBaseUrl(), getEmbeddingPath())); }
    default String getResolvedRerankRequestUrl() { return toRerankRequestUrl(join(getBaseUrl(), getRerankPath())); }

    private static String join(String baseUrl, String path) {
        String base = normalizeBase(baseUrl);
        String p = path == null ? "" : path.trim();
        if (p.isBlank()) {
            return base;
        }
        if (p.startsWith("http://") || p.startsWith("https://")) {
            return normalizeBase(p);
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return base + p;
    }

    private static String normalizeBase(String value) {
        String v = value == null ? "" : value.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String toChatCompletionUrl(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/v1/chat/completions") || v.endsWith("/chat/completions")) {
            return v;
        }
        if (v.endsWith("/v1")) {
            return v + "/chat/completions";
        }
        return v + "/v1/chat/completions";
    }

    private static String toEmbeddingRequestUrl(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/api/v1/embeddings") || v.endsWith("/v1/embeddings") || v.endsWith("/embeddings")) {
            return v;
        }
        if (v.endsWith("/api/v1") || v.endsWith("/v1")) {
            return v + "/embeddings";
        }
        if (v.contains("/embedding/api")) {
            return v + "/v1/embeddings";
        }
        return v + "/api/v1/embeddings";
    }

    private static String toRerankRequestUrl(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/rerank/rerank") || v.endsWith("/api/v1/rerank")) {
            return v;
        }
        if (v.endsWith("/rerank")) {
            return v + "/rerank";
        }
        return v + "/rerank";
    }

    private static String toOpenAiBase(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/v1/chat/completions")) {
            return v.substring(0, v.length() - "/v1/chat/completions".length());
        }
        if (v.endsWith("/chat/completions")) {
            return v.substring(0, v.length() - "/chat/completions".length());
        }
        if (v.endsWith("/v1")) {
            return v.substring(0, v.length() - "/v1".length());
        }
        return v;
    }

    private static String toEmbeddingBase(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/api/v1/embeddings")) {
            return v.substring(0, v.length() - "/v1/embeddings".length()); // keep /api prefix
        }
        if (v.endsWith("/v1/embeddings")) {
            return v.substring(0, v.length() - "/v1/embeddings".length());
        }
        if (v.endsWith("/embeddings")) {
            return v.substring(0, v.length() - "/embeddings".length());
        }
        if (v.endsWith("/api/v1")) {
            return v.substring(0, v.length() - "/v1".length());
        }
        if (v.endsWith("/v1")) {
            return v.substring(0, v.length() - "/v1".length());
        }
        return v;
    }

    private static String toRerankBase(String url) {
        String v = normalizeBase(url);
        if (v.endsWith("/rerank/rerank")) {
            return v.substring(0, v.length() - "/rerank".length());
        }
        if (v.endsWith("/api/v1/rerank")) {
            return v.substring(0, v.length() - "/api/v1/rerank".length());
        }
        return v;
    }
}
