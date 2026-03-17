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

import ai.local.nalbbun.internal.service.DebugRuntimeOllamaConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runtime Ollama Vector Store Factory 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
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

    /**
     * create 객체를 생성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * runtime Rest Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Math.max(1000, ollamaConnectTimeoutMs));
        requestFactory.setReadTimeout((int) Math.max(Math.max(1000, ollamaConnectTimeoutMs), ollamaRequestTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * runtime Web Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
