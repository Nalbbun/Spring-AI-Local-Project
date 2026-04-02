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
    default String getSllmBaseUrl() { return join(getBaseUrl(), getSllmPath()); }
    default String getLlmBaseUrl() { return join(getBaseUrl(), getLlmPath()); }
    default String getEmbeddingBaseUrl() { return join(getBaseUrl(), getEmbeddingPath()); }
    default String getRerankBaseUrl() { return join(getBaseUrl(), getRerankPath()); }
    private static String join(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String p = path == null ? "" : path.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!p.isBlank() && !p.startsWith("/")) p = "/" + p;
        return base + p;
    }
}
