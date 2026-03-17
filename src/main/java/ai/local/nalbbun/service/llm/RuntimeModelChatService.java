package ai.local.nalbbun.service.llm;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import lombok.extern.slf4j.Slf4j;

/**
 * RuntimeModelChatService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: runtime model chat service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Slf4j
@Service
public class RuntimeModelChatService {

    /** openaiBuilder 값을 보관한다. */
    private final ChatClient.Builder openaiBuilder;
    /** runtimeModelResolver 값을 보관한다. */
    private final RuntimeModelResolver runtimeModelResolver;
    /** llmTaskExecutor 값을 보관한다. */
    private final Executor llmTaskExecutor;
    /** timeoutMs 값을 보관한다. */
    private final long timeoutMs;
    /** retryAttempts 값을 보관한다. */
    private final int retryAttempts;
    /** retryBackoffMs 값을 보관한다. */
    private final long retryBackoffMs;
    /** ollamaConnectTimeoutMs 값을 보관한다. */
    private final long ollamaConnectTimeoutMs;
    /** ollamaRequestTimeoutMs 값을 보관한다. */
    private final long ollamaRequestTimeoutMs;
    /** ollamaChatKeepAlive 값을 보관한다. */
    private final String ollamaChatKeepAlive;
    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param openaiBuilder openaiBuilder 값
     * @param runtimeModelResolver runtimeModelResolver 값
     * @param llmTaskExecutor llmTaskExecutor 값
     * @param ollamaConnectionService ollamaConnectionService 값
     * @param timeoutMs timeoutMs 값
     * @param retryAttempts retryAttempts 값
     * @param retryBackoffMs retryBackoffMs 값
     * @param ollamaConnectTimeoutMs ollamaConnectTimeoutMs 값
     * @param ollamaRequestTimeoutMs ollamaRequestTimeoutMs 값
     * @param ollamaChatKeepAlive ollamaChatKeepAlive 값
     */
    public RuntimeModelChatService(
            @Qualifier("openaiBuilder") ChatClient.Builder openaiBuilder,
            RuntimeModelResolver runtimeModelResolver,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            @Value("${app.llm.timeout-ms:45000}") long timeoutMs,
            @Value("${app.llm.retry-attempts:2}") int retryAttempts,
            @Value("${app.llm.retry-backoff-ms:800}") long retryBackoffMs,
            @Value("${app.ollama.connect-timeout-ms:5000}") long ollamaConnectTimeoutMs,
            @Value("${app.ollama.request-timeout-ms:300000}") long ollamaRequestTimeoutMs,
            @Value("${spring.ai.ollama.chat.options.keep-alive:300s}") String ollamaChatKeepAlive
    ) {
        this.openaiBuilder = openaiBuilder;
        this.runtimeModelResolver = runtimeModelResolver;
        this.llmTaskExecutor = llmTaskExecutor;
        this.ollamaConnectionService = ollamaConnectionService;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = Math.max(1, retryAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        this.ollamaConnectTimeoutMs = Math.max(1000, ollamaConnectTimeoutMs);
        this.ollamaRequestTimeoutMs = Math.max(this.ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);
        this.ollamaChatKeepAlive = (ollamaChatKeepAlive == null || ollamaChatKeepAlive.isBlank()) ? "300s" : ollamaChatKeepAlive.trim();
    }

    /**
     * callText 기능을 수행한다.
     *
     * @param target target 값
     * @param systemPrompt systemPrompt 값
     * @param userPrompt userPrompt 값
     * @return 처리 결과 문자열
     */
    public String callText(RuntimeModelTarget target, String systemPrompt, String userPrompt) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> {
            if (resolved.ollama()) {
                ChatClient client = runtimeOllamaClient(systemPrompt, resolved.modelName());
                return client.prompt()
                        .user(userPrompt)
                        .call()
                        .content();
            }

            ChatClient client = openaiBuilder.defaultSystem(systemPrompt).build();
            return client.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
        });
    }

    /**
     * callEntity 기능을 수행한다.
     *
     * @param target target 값
     * @param systemPrompt systemPrompt 값
     * @param userPrompt userPrompt 값
     * @param responseType responseType 값
     * @return T 타입의 처리 결과
     */
    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            Class<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> {
            if (resolved.ollama()) {
                ChatClient client = runtimeOllamaClient(systemPrompt, resolved.modelName());
                return client.prompt()
                        .user(userPrompt)
                        .call()
                        .entity(responseType);
            }

            ChatClient client = openaiBuilder.defaultSystem(systemPrompt).build();
            return client.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
        });
    }

    /**
     * callEntity 기능을 수행한다.
     *
     * @param target target 값
     * @param systemPrompt systemPrompt 값
     * @param userPrompt userPrompt 값
     * @param responseType responseType 값
     * @return T 타입의 처리 결과
     */
    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            ParameterizedTypeReference<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> {
            if (resolved.ollama()) {
                ChatClient client = runtimeOllamaClient(systemPrompt, resolved.modelName());
                return client.prompt()
                        .user(userPrompt)
                        .call()
                        .entity(responseType);
            }

            ChatClient client = openaiBuilder.defaultSystem(systemPrompt).build();
            return client.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
        });
    }

    /**
     * callEntityWithTools 기능을 수행한다.
     *
     * @param target target 값
     * @param systemPrompt systemPrompt 값
     * @param userPrompt userPrompt 값
     * @param toolObject toolObject 값
     * @param responseType responseType 값
     * @return T 타입의 처리 결과
     */
    public <T> T callEntityWithTools(RuntimeModelTarget target,
                                     String systemPrompt,
                                     String userPrompt,
                                     Object toolObject,
                                     ParameterizedTypeReference<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, true);
        return executeWithPolicy(target, resolved, () -> {
            if (resolved.ollama()) {
                ChatClient client = runtimeOllamaClient(systemPrompt, resolved.modelName());
                return client.prompt()
                        .user(userPrompt)
                        .tools(toolObject)
                        .call()
                        .entity(responseType);
            }

            ChatClient client = openaiBuilder.defaultSystem(systemPrompt).build();
            return client.prompt()
                    .user(userPrompt)
                    .tools(toolObject)
                    .call()
                    .entity(responseType);
        });
    }

    /**
     * describeResolvedModel 기능을 수행한다.
     *
     * @param target target 값
     * @param requiresTools requiresTools 값
     * @return 처리 결과 문자열
     */
    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        return runtimeModelResolver.resolve(target, requiresTools).describe();
    }

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param systemPrompt systemPrompt 값
     * @param modelName modelName 값
     * @return ChatClient 타입의 처리 결과
     */
    private ChatClient runtimeOllamaClient(String systemPrompt, String modelName) {
        String baseUrl = ollamaConnectionService.getBaseUrl();
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(runtimeRestClientBuilder())
                .webClientBuilder(runtimeWebClientBuilder())
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(modelName)
                        .keepAlive(ollamaChatKeepAlive)
                        .build())
                .build();
        log.info("Using runtime Ollama connection. baseUrl={}, model={}, keepAlive={}, connectTimeoutMs={}, requestTimeoutMs={}",
                baseUrl, modelName, ollamaChatKeepAlive, ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 핵심 처리 로직을 실행한다.
     * @return RestClient.Builder 타입의 처리 결과
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) ollamaConnectTimeoutMs);
        requestFactory.setReadTimeout((int) ollamaRequestTimeoutMs);
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * 핵심 처리 로직을 실행한다.
     * @return WebClient.Builder 타입의 처리 결과
     */
    private WebClient.Builder runtimeWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param target target 값
     * @param resolved resolved 값
     * @param supplier supplier 값
     * @return T 타입의 처리 결과
     */
    private <T> T executeWithPolicy(RuntimeModelTarget target,
                                    RuntimeModelSelection resolved,
                                    Supplier<T> supplier) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                if (resolved.fallbackApplied()) {
                    log.warn("LLM fallback applied. target={}, resolution={}", target, resolved.describe());
                } else {
                    log.debug("LLM call. target={}, resolution={}", target, resolved.describe());
                }

                long effectiveTimeoutMs = resolved.ollama()
                        ? Math.max(timeoutMs, ollamaRequestTimeoutMs + 5_000L)
                        : timeoutMs;
                return CompletableFuture.supplyAsync(supplier, llmTaskExecutor)
                        .orTimeout(effectiveTimeoutMs, TimeUnit.MILLISECONDS)
                        .join();
            } catch (Exception e) {
                lastException = unwrap(e);
                log.warn("LLM call failed. target={}, attempt={}/{}, reason={}",
                        target, attempt, retryAttempts, lastException.getMessage());
                sleepBackoff(attempt);
            }
        }

        throw new IllegalStateException(
                "LLM 호출 실패: target=%s, timeoutMs=%d, attempts=%d, cause=%s"
                        .formatted(target, timeoutMs, retryAttempts, lastException == null ? "unknown" : lastException.getMessage()),
                lastException
        );
    }

    /**
     * unwrap 기능을 수행한다.
     *
     * @param exception exception 값
     * @return Exception 타입의 처리 결과
     */
    private Exception unwrap(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current instanceof RuntimeException) {
            current = current.getCause();
        }
        return current instanceof Exception e ? e : new RuntimeException(current);
    }

    /**
     * sleepBackoff 기능을 수행한다.
     *
     * @param attempt attempt 값
     */
    private void sleepBackoff(int attempt) {
        if (attempt >= retryAttempts || retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
