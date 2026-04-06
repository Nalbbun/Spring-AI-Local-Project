package ai.local.nalbbun.domain.rag.service;

import java.time.Duration;
import java.util.Locale;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import ai.local.nalbbun.domain.runtime.port.RuntimeOllamaConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeOpenAiConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeOllamaVectorStoreFactory {
    @Qualifier("vectorJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;
    private final RuntimeOllamaConnectionPort ollamaConnectionService;
    private final RuntimeVllmConnectionPort vllmConnectionService;
    private final RuntimeOpenAiConnectionPort openAiConnectionService;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModelName;
    @Value("${spring.ai.vectorstore.pgvector.initialize-schema:true}")
    private boolean initializeSchema;
    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int dimensions;
    @Value("${spring.ai.ollama.embedding.options.keep-alive:300s}")
    private String embeddingKeepAlive;
    @Value("${app.ollama.connect-timeout-ms:5000}")
    private long ollamaConnectTimeoutMs;
    @Value("${app.ollama.request-timeout-ms:300000}")
    private long ollamaRequestTimeoutMs;

    public void setEmbeddingModel(String model) { if (model != null && !model.isBlank()) { this.embeddingModelName = model.trim(); log.info("Embedding model changed to: {}", this.embeddingModelName);} }
    public void setEmbeddingKeepAlive(String keepAlive) { if (keepAlive != null && !keepAlive.isBlank()) this.embeddingKeepAlive = keepAlive.trim(); }
    public void setDimensions(int dimensions) { if (dimensions > 0) this.dimensions = dimensions; }
    public String getEmbeddingModelName() { return embeddingModelName; }
    public String getEmbeddingKeepAlive() { return embeddingKeepAlive; }
    public int getDimensions() { return dimensions; }

    public VectorStore create() {
        EmbeddingModel embeddingModel = createEmbeddingModel();
        log.info("Creating runtime PGVector store. embeddingModel={}, keepAlive={}, dimensions={}", embeddingModelName, embeddingKeepAlive, dimensions);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).dimensions(dimensions).initializeSchema(initializeSchema).build();
    }

    private EmbeddingModel createEmbeddingModel() {
        ProviderSelection selection = parseProviderSelection(embeddingModelName);
        if ("VLLM".equals(selection.provider())) {
            return new VllmGatewayEmbeddingModel(vllmConnectionService, dimensions, ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);
        }
        if ("OPENAI".equals(selection.provider())) {
            OpenAiApi api = OpenAiApi.builder().baseUrl(sanitizeOpenAiBaseUrl(openAiConnectionService.getBaseUrl())).apiKey(openAiConnectionService.getResolvedApiKey()).restClientBuilder(runtimeRestClientBuilder()).webClientBuilder(runtimeWebClientBuilder()).build();
            return new OpenAiEmbeddingModel(api, MetadataMode.NONE, OpenAiEmbeddingOptions.builder().model(selection.model()).build());
        }
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(ollamaConnectionService.getBaseUrl()).restClientBuilder(runtimeRestClientBuilder()).webClientBuilder(runtimeWebClientBuilder()).build();
        return OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(OllamaEmbeddingOptions.builder().model(selection.model()).keepAlive((embeddingKeepAlive == null || embeddingKeepAlive.isBlank()) ? "300s" : embeddingKeepAlive.trim()).build()).build();
    }

    private ProviderSelection parseProviderSelection(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.toUpperCase(Locale.ROOT).startsWith("VLLM::")) return new ProviderSelection("VLLM", value.substring("VLLM::".length()).trim());
        if (value.toUpperCase(Locale.ROOT).startsWith("OPENAI::")) return new ProviderSelection("OPENAI", value.substring("OPENAI::".length()).trim());
        if (value.toUpperCase(Locale.ROOT).startsWith("OLLAMA::")) return new ProviderSelection("OLLAMA", value.substring("OLLAMA::".length()).trim());
        return new ProviderSelection("OLLAMA", value);
    }

    private String sanitizeOpenAiBaseUrl(String baseUrl) { String value = baseUrl == null ? "https://api.openai.com" : baseUrl.trim(); if (value.isBlank()) return "https://api.openai.com"; if (value.endsWith("/")) value = value.substring(0, value.length()-1); if (value.endsWith("/v1")) value = value.substring(0, value.length()-3); return value; }
    private RestClient.Builder runtimeRestClientBuilder() { SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory(); requestFactory.setConnectTimeout((int)Math.max(1000, ollamaConnectTimeoutMs)); requestFactory.setReadTimeout((int)Math.max(Math.max(1000, ollamaConnectTimeoutMs), ollamaRequestTimeoutMs)); return RestClient.builder().requestFactory(requestFactory); }
    private WebClient.Builder runtimeWebClientBuilder() { long connectTimeout = Math.max(1000, ollamaConnectTimeoutMs); long requestTimeout = Math.max(connectTimeout, ollamaRequestTimeoutMs); HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout).responseTimeout(Duration.ofMillis(requestTimeout)); return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)); }
    private record ProviderSelection(String provider, String model) {}
}
