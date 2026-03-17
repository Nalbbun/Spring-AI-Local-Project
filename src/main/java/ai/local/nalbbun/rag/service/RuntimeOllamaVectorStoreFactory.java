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

/**
 * RuntimeOllamaVectorStoreFactory는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: runtime ollama vector store factory 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeOllamaVectorStoreFactory {

    /** jdbcTemplate 값을 보관한다. */
    private final JdbcTemplate jdbcTemplate;
    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    /** embeddingModelName 값을 보관한다. */
    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModelName;

    /** initializeSchema 값을 보관한다. */
    @Value("${spring.ai.vectorstore.pgvector.initialize-schema:true}")
    private boolean initializeSchema;

    /** dimensions 값을 보관한다. */
    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int dimensions;

    /** embeddingKeepAlive 값을 보관한다. */
    @Value("${spring.ai.ollama.embedding.options.keep-alive:300s}")
    private String embeddingKeepAlive;

    /** ollamaConnectTimeoutMs 값을 보관한다. */
    @Value("${app.ollama.connect-timeout-ms:5000}")
    private long ollamaConnectTimeoutMs;

    /** ollamaRequestTimeoutMs 값을 보관한다. */
    @Value("${app.ollama.request-timeout-ms:300000}")
    private long ollamaRequestTimeoutMs;

    /**
     * 새 항목 또는 결과를 생성한다.
     * @return VectorStore 타입의 처리 결과
     */
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
    /**
     * 핵심 처리 로직을 실행한다.
     * @return RestClient.Builder 타입의 처리 결과
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Math.max(1000, ollamaConnectTimeoutMs));
        requestFactory.setReadTimeout((int) Math.max(Math.max(1000, ollamaConnectTimeoutMs), ollamaRequestTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * 핵심 처리 로직을 실행한다.
     * @return WebClient.Builder 타입의 처리 결과
     */
    private WebClient.Builder runtimeWebClientBuilder() {
        long connectTimeout = Math.max(1000, ollamaConnectTimeoutMs);
        long requestTimeout = Math.max(connectTimeout, ollamaRequestTimeoutMs);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout)
                .responseTimeout(Duration.ofMillis(requestTimeout));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

}
