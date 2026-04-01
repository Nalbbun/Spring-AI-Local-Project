package ai.local.nalbbun.domain.runtime.port;

public interface RuntimeVllmConnectionPort {
    String getBaseUrl();
    String getConfiguredOrDefaultModel();
    String getKeyProvider();
    String getResolvedApiKey();
}
