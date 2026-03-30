package ai.local.nalbbun.domain.runtime.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.admin.service.DebugRuntimeOllamaConnectionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Runtime Model Chat Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Slf4j
@Service
public class RuntimeModelChatService {

    private final ChatClient.Builder openaiBuilder;
    private final RuntimeModelResolver runtimeModelResolver;
    private final Executor llmTaskExecutor;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final long ollamaConnectTimeoutMs;
    private final long ollamaRequestTimeoutMs;
    private final String ollamaChatKeepAlive;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    /**
     * Runtime Model Chat Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * 토큰 단위 스트리밍 호출.
     * 각 토큰이 생성될 때마다 tokenConsumer 를 호출하고,
     * 완성된 전체 텍스트를 반환합니다.
     */
    public String streamText(RuntimeModelTarget target,
                             String systemPrompt,
                             String userPrompt,
                             Consumer<String> tokenConsumer) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);

        if (resolved.fallbackApplied()) {
            log.warn("LLM streaming fallback applied. target={}, resolution={}", target, resolved.describe());
        }

        StringBuilder fullResponse = new StringBuilder();

        try {
            if (resolved.ollama()) {
                ChatClient client = runtimeOllamaClient(systemPrompt, resolved.modelName());
                client.prompt()
                        .user(userPrompt)
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            fullResponse.append(token);
                            tokenConsumer.accept(token);
                        })
                        .blockLast(Duration.ofMillis(
                                Math.max(timeoutMs, ollamaRequestTimeoutMs + 5_000L)));
            } else {
                ChatClient client = openaiBuilder.defaultSystem(systemPrompt).build();
                client.prompt()
                        .user(userPrompt)
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            fullResponse.append(token);
                            tokenConsumer.accept(token);
                        })
                        .blockLast(Duration.ofMillis(timeoutMs));
            }
        } catch (Exception e) {
            log.warn("LLM stream failed. target={}, reason={}", target, e.getMessage());
            // 스트리밍 중 오류 시 지금까지 수집된 텍스트라도 반환
            if (fullResponse.isEmpty()) {
                throw new IllegalStateException(
                        "LLM 스트리밍 실패: target=%s, cause=%s".formatted(target, e.getMessage()), e);
            }
        }

        return fullResponse.toString();
    }

    /**
     * call Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * call Entity 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * call Entity 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * call Entity With Tools 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * describe Resolved Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        return runtimeModelResolver.resolve(target, requiresTools).describe();
    }

    /**
     * runtime Ollama Client 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * runtime Rest Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        // Spring 6.1+ : SimpleClientHttpRequestFactory → JdkClientHttpRequestFactory 권장
        // connectTimeout / readTimeout 모두 Duration으로 명시
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * runtime Web Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private WebClient.Builder runtimeWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * execute With Policy 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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

                // 복잡한 에이전트 target(TRAVEL_PLAN)은 더 긴 타임아웃 적용
                long effectiveTimeoutMs = resolveEffectiveTimeout(target, resolved);
                log.debug("LLM timeout. target={}, effectiveTimeoutMs={}", target, effectiveTimeoutMs);

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
                        .formatted(target, timeoutMs, retryAttempts,
                                   lastException != null ? lastException.getMessage() : "unknown"),
                lastException
        );
    }

    /**
     * target 과 provider 에 따른 실효 타임아웃을 계산합니다.
     * - Ollama: ollamaRequestTimeoutMs + 여유 5초 (기본 300초)
     * - OpenAI (일반): timeoutMs (기본 180초)
     * - OpenAI TRAVEL_PLAN: timeoutMs * 2 (일정 생성은 프롬프트가 길어 시간이 더 걸림)
     */
    private long resolveEffectiveTimeout(RuntimeModelTarget target, RuntimeModelSelection resolved) {
        if (resolved.ollama()) {
            return Math.max(timeoutMs, ollamaRequestTimeoutMs + 5_000L);
        }
        // OpenAI fallback 또는 OPENAI_ONLY 정책
        if (target == RuntimeModelTarget.TRAVEL_PLAN) {
            return timeoutMs * 2;   // 일정 생성 프롬프트 길이 대응
        }
        return timeoutMs;
    }

    private Exception unwrap(Exception exception) {
        // CompletableFuture.join() 은 CompletionException 으로 래핑 → 원인을 unwrap
        Throwable current = exception;
        while (current.getCause() != null
               && (current instanceof java.util.concurrent.CompletionException
                   || current instanceof RuntimeException)) {
            current = current.getCause();
        }
        return current instanceof Exception e ? e : new RuntimeException(current);
    }

    /**
     * sleep Backoff 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
