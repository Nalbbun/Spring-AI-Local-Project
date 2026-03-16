package ai.local.nalbbun.service.llm;

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
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RuntimeModelChatService {

    private final ChatClient.Builder openaiBuilder;
    private final RuntimeModelResolver runtimeModelResolver;
    private final Executor llmTaskExecutor;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;

    public RuntimeModelChatService(
            @Qualifier("openaiBuilder") ChatClient.Builder openaiBuilder,
            RuntimeModelResolver runtimeModelResolver,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            @Value("${app.llm.timeout-ms:45000}") long timeoutMs,
            @Value("${app.llm.retry-attempts:2}") int retryAttempts,
            @Value("${app.llm.retry-backoff-ms:800}") long retryBackoffMs
    ) {
        this.openaiBuilder = openaiBuilder;
        this.runtimeModelResolver = runtimeModelResolver;
        this.llmTaskExecutor = llmTaskExecutor;
        this.ollamaConnectionService = ollamaConnectionService;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = Math.max(1, retryAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

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

    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        return runtimeModelResolver.resolve(target, requiresTools).describe();
    }

    private ChatClient runtimeOllamaClient(String systemPrompt, String modelName) {
        String baseUrl = ollamaConnectionService.getBaseUrl();
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(modelName)
                        .build())
                .build();
        log.debug("Using runtime Ollama connection. baseUrl={}, model={}", baseUrl, modelName);
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
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

                return CompletableFuture.supplyAsync(supplier, llmTaskExecutor)
                        .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
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

    private Exception unwrap(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current instanceof RuntimeException) {
            current = current.getCause();
        }
        return current instanceof Exception e ? e : new RuntimeException(current);
    }

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
