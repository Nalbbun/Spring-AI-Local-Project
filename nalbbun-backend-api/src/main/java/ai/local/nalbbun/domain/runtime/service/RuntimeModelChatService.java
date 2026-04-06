package ai.local.nalbbun.domain.runtime.service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import ai.local.nalbbun.domain.runtime.model.RuntimeLlmProvider;
import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.domain.runtime.port.RuntimeOllamaConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeOpenAiConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

/**
 * Runtime Model Chat Service 타입이다.
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
    private final RuntimeOllamaConnectionPort ollamaConnectionService;
    private final RuntimeVllmConnectionPort vllmConnectionService;
    private final RuntimeOpenAiConnectionPort openAiConnectionService;

    public RuntimeModelChatService(
            @Qualifier("openaiBuilder") ChatClient.Builder openaiBuilder,
            RuntimeModelResolver runtimeModelResolver,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            RuntimeOllamaConnectionPort ollamaConnectionService,
            RuntimeVllmConnectionPort vllmConnectionService,
            RuntimeOpenAiConnectionPort openAiConnectionService,
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
        this.vllmConnectionService = vllmConnectionService;
        this.openAiConnectionService = openAiConnectionService;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = Math.max(1, retryAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        this.ollamaConnectTimeoutMs = Math.max(1000, ollamaConnectTimeoutMs);
        this.ollamaRequestTimeoutMs = Math.max(this.ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);
        this.ollamaChatKeepAlive = (ollamaChatKeepAlive == null || ollamaChatKeepAlive.isBlank()) ? "300s" : ollamaChatKeepAlive.trim();
    }

    public String streamText(RuntimeModelTarget target,
                             String systemPrompt,
                             String userPrompt,
                             Consumer<String> tokenConsumer) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        log.info("LLM stream start. target={}, resolution={}", target, resolved.describe());

        if (resolved.fallbackApplied()) {
            log.warn("LLM streaming fallback applied. target={}, resolution={}", target, resolved.describe());
        }

        StringBuilder fullResponse = new StringBuilder();

        try {
            ChatClient client = runtimeChatClient(systemPrompt, resolved);
            client.prompt()
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        fullResponse.append(token);
                        tokenConsumer.accept(token);
                    })
                    .blockLast(Duration.ofMillis(Math.max(timeoutMs, ollamaRequestTimeoutMs + 5_000L)));
        } catch (Exception e) {
            log.error("LLM stream failed. target={}, resolution={}, partialLength={}, reason={}",
                    target, resolved.describe(), fullResponse.length(), e.getMessage(), e);
            if (resolved.provider() == RuntimeLlmProvider.VLLM && fullResponse.isEmpty()) {
                try {
                    String fallback = runtimeChatClient(systemPrompt, resolved).prompt().user(userPrompt).call().content();
                    if (fallback != null && !fallback.isBlank()) {
                        fullResponse.append(fallback);
                        tokenConsumer.accept(fallback);
                    }
                } catch (Exception fallbackError) {
                    throw new IllegalStateException(
                            "LLM 스트리밍 실패: target=%s, cause=%s".formatted(target, fallbackError.getMessage()), fallbackError);
                }
            } else if (fullResponse.isEmpty()) {
                throw new IllegalStateException(
                        "LLM 스트리밍 실패: target=%s, cause=%s".formatted(target, e.getMessage()), e);
            }
        }

        log.info("LLM stream end. target={}, resolution={}, responseLength={}",
                target, resolved.describe(), fullResponse.length());
        return fullResponse.toString();
    }

    public String callText(RuntimeModelTarget target, String systemPrompt, String userPrompt) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> runtimeChatClient(systemPrompt, resolved).prompt()
                .user(userPrompt)
                .call()
                .content());
    }

    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            Class<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> runtimeChatClient(systemPrompt, resolved).prompt()
                .user(userPrompt)
                .call()
                .entity(responseType));
    }

    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            ParameterizedTypeReference<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> runtimeChatClient(systemPrompt, resolved).prompt()
                .user(userPrompt)
                .call()
                .entity(responseType));
    }

    public <T> T callEntityWithTools(RuntimeModelTarget target,
                                     String systemPrompt,
                                     String userPrompt,
                                     Object toolObject,
                                     ParameterizedTypeReference<T> responseType) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, true);
        return executeWithPolicy(target, resolved, () -> runtimeChatClient(systemPrompt, resolved).prompt()
                .user(userPrompt)
                .tools(toolObject)
                .call()
                .entity(responseType));
    }

    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        return runtimeModelResolver.resolve(target, requiresTools).describe();
    }

    private ChatClient runtimeChatClient(String systemPrompt, RuntimeModelSelection resolved) {
        if (resolved.provider() == RuntimeLlmProvider.OLLAMA) {
            return runtimeOllamaClient(systemPrompt, resolved.modelName());
        }
        if (resolved.provider() == RuntimeLlmProvider.OPENAI) {
            return runtimeOpenAiClient(systemPrompt, resolved);
        }
        return runtimeApiCompatibleClient(systemPrompt, resolved);
    }

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



    private ChatClient runtimeOpenAiClient(String systemPrompt, RuntimeModelSelection resolved) {
        String apiKey = openAiConnectionService.getResolvedApiKey();
        String sanitizedBaseUrl = sanitizeOpenAiBaseUrl(resolved.baseUrl());
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(sanitizedBaseUrl)
                .apiKey(apiKey)
                .restClientBuilder(runtimeRestClientBuilder())
                .webClientBuilder(runtimeWebClientBuilder())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(resolved.modelName()).build())
                .build();
        log.info("Using runtime native OpenAI connection. baseUrl={}, model={}, keyProvider={}",
                sanitizedBaseUrl, resolved.modelName(), resolved.keyProvider());
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
    }

    private ChatClient runtimeApiCompatibleClient(String systemPrompt, RuntimeModelSelection resolved) {
        String apiKey = resolved.provider() == RuntimeLlmProvider.VLLM
                ? vllmConnectionService.getResolvedApiKey()
                : openAiConnectionService.getResolvedApiKey();
        String resolvedModelName = resolved.provider() == RuntimeLlmProvider.VLLM ? normalizeVllmModelAlias(resolved.modelName()) : resolved.modelName();
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(resolved.baseUrl())
                .apiKey(apiKey)
                .restClientBuilder(runtimeRestClientBuilder())
                .webClientBuilder(runtimeWebClientBuilder())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(resolvedModelName).build())
                .build();
        log.info("Using runtime API-compatible connection. provider={}, baseUrl={}, model={}, keyProvider={}",
                resolved.provider(), resolved.baseUrl(), resolvedModelName, resolved.keyProvider());
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
    }



    private String sanitizeOpenAiBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "https://api.openai.com" : baseUrl.trim();
        if (value.isBlank()) return "https://api.openai.com";
        if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.endsWith("/chat/completions")) value = value.substring(0, value.length() - "/chat/completions".length());
        if (value.endsWith("/v1")) value = value.substring(0, value.length() - 3);
        return value;
    }

    private RestClient.Builder runtimeRestClientBuilder() {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Math.max(1000, ollamaConnectTimeoutMs));
        requestFactory.setReadTimeout((int) Math.max(Math.max(1000, ollamaConnectTimeoutMs), ollamaRequestTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }

    private WebClient.Builder runtimeWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

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

                long effectiveTimeoutMs = resolveEffectiveTimeout(target, resolved);
                log.debug("LLM timeout. target={}, effectiveTimeoutMs={}", target, effectiveTimeoutMs);

                T result = CompletableFuture.supplyAsync(() -> {
                            try {
                                return supplier.get();
                            } catch (Exception ex) {
                                log.error("LLM supplier failed inside executor. target={}, resolution={}, reason={}",
                                        target, resolved.describe(), ex.getMessage(), ex);
                                throw ex;
                            }
                        }, llmTaskExecutor)
                        .orTimeout(effectiveTimeoutMs, TimeUnit.MILLISECONDS)
                        .join();
                log.info("LLM call success. target={}, attempt={}, resolution={}", target, attempt, resolved.describe());
                return result;
            } catch (Exception e) {
                lastException = unwrap(e);
                log.error("LLM call failed. target={}, attempt={}/{}, resolution={}, reason={}",
                        target, attempt, retryAttempts, resolved.describe(), lastException.getMessage(), lastException);
                sleepBackoff(attempt);
            }
        }

        throw new IllegalStateException(
                "LLM 호출 실패: target=%s, timeoutMs=%d, attempts=%d, cause=%s"
                        .formatted(target, timeoutMs, retryAttempts, lastException == null ? "unknown" : lastException.getMessage()),
                lastException);
    }

    private long resolveEffectiveTimeout(RuntimeModelTarget target, RuntimeModelSelection resolved) {
        long effective = timeoutMs;
        if (target == RuntimeModelTarget.TRAVEL_PLAN) {
            effective = Math.max(effective, 90_000L);
        }
        if (resolved.ollama()) {
            effective = Math.max(effective, ollamaRequestTimeoutMs + 2_000L);
        }
        return effective;
    }

    private void sleepBackoff(int attempt) {
        if (retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Exception unwrap(Exception e) {
        Throwable current = e;
        while (current.getCause() != null && current instanceof java.util.concurrent.CompletionException) {
            current = current.getCause();
        }
        return current instanceof Exception ex ? ex : e;
    }

    private String normalizeVllmModelAlias(String modelName) {
        if (modelName == null) return null;
        String normalized = modelName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("exaone-3.5-2.4b")) return "exaone-3.5-2.4b-it";
        if (normalized.contains("exaone-3.5-32b")) return "exaone-3.5-32b-it";
        if (normalized.contains("bge-reranker-v2-m3")) return "bge-reranker-v2-m3";
        if (normalized.contains("bge-m3")) return "bge-m3";
        return modelName;
    }
}


