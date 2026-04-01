package ai.local.nalbbun.domain.runtime.port;

public interface RuntimeOpenAiConnectionPort {
    String getBaseUrl();
    String getConfiguredOrDefaultModel();
    String getKeyProvider();
    String getResolvedApiKey();
}
