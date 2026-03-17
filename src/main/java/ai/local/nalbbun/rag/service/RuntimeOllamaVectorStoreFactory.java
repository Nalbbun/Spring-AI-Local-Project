package ai.local.nalbbun.rag.service;

import java.time.Duration;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeOllamaVectorStoreFactory {

    private final JdbcTemplate jdbcTemplate;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

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

    public VectorStore create() {
        String baseUrl = ollamaConnectionService.getBaseUrl();
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(runtimeRestClientBuilder())
                .webClientBuilder(runtimeWebClientBuilder())
                .build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaEmbeddingOptions.builder()
                        .model(embeddingModelName)
                        .keepAlive((embeddingKeepAlive == null || embeddingKeepAlive.isBlank()) ? "300s" : embeddingKeepAlive.trim())
                        .build())
                .build();

        log.info("Creating runtime PGVector store. baseUrl={}, embeddingModel={}, keepAlive={}, dimensions={}, connectTimeoutMs={}, requestTimeoutMs={}",
                baseUrl, embeddingModelName, embeddingKeepAlive, dimensions, ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .initializeSchema(initializeSchema)
                .build();
    }
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Math.max(1000, ollamaConnectTimeoutMs));
        requestFactory.setReadTimeout((int) Math.max(Math.max(1000, ollamaConnectTimeoutMs), ollamaRequestTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }

    private WebClient.Builder runtimeWebClientBuilder() {
        long connectTimeout = Math.max(1000, ollamaConnectTimeoutMs);
        long requestTimeout = Math.max(connectTimeout, ollamaRequestTimeoutMs);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout)
                .responseTimeout(Duration.ofMillis(requestTimeout));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

}
