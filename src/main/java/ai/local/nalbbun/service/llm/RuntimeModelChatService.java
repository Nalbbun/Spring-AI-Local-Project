package ai.local.nalbbun.service.llm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RuntimeModelChatService {

    private final ChatClient.Builder openaiBuilder;
    private final ChatClient.Builder ollamaBuilder;
    private final RuntimeModelResolver runtimeModelResolver;
    private final Executor llmTaskExecutor;
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    public RuntimeModelChatService(
            @Qualifier("openaiBuilder") ChatClient.Builder openaiBuilder,
            @Qualifier("ollamaBuilder") ChatClient.Builder ollamaBuilder,
            RuntimeModelResolver runtimeModelResolver,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.openaiBuilder = openaiBuilder;
        this.ollamaBuilder = ollamaBuilder;
        this.runtimeModelResolver = runtimeModelResolver;
        this.llmTaskExecutor = llmTaskExecutor;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    public String callText(RuntimeModelTarget target, String systemPrompt, String userPrompt) {
        RuntimeModelSelection resolved = runtimeModelResolver.resolve(target, false);
        return executeWithPolicy(target, resolved, () -> {
            if (resolved.ollama()) {
                ChatClient client = ollamaBuilder.defaultSystem(systemPrompt).build();
                return client.prompt()
                        .user(userPrompt)
                        .options(OllamaChatOptions.builder()
                                .model(resolved.modelName())
                                .build())
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
                ChatClient client = ollamaBuilder.defaultSystem(systemPrompt).build();
                return client.prompt()
                        .user(userPrompt)
                        .options(OllamaChatOptions.builder()
                                .model(resolved.modelName())
                                .build())
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
                ChatClient client = ollamaBuilder.defaultSystem(systemPrompt).build();
                return client.prompt()
                        .user(userPrompt)
                        .options(OllamaChatOptions.builder()
                                .model(resolved.modelName())
                                .build())
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
                ChatClient client = ollamaBuilder.defaultSystem(systemPrompt).build();
                return client.prompt()
                        .user(userPrompt)
                        .tools(toolObject)
                        .options(OllamaChatOptions.builder()
                                .model(resolved.modelName())
                                .build())
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

    private <T> T executeWithPolicy(RuntimeModelTarget target,
                                    RuntimeModelSelection resolved,
                                    Supplier<T> supplier) {
        Exception lastException = null;
        int retryAttempts = debugRuntimeConfigService.getLlmRetryAttempts();
        long timeoutMs = debugRuntimeConfigService.getLlmTimeoutMs();

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
                sleepBackoff(attempt, retryAttempts);
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

    private void sleepBackoff(int attempt, int retryAttempts) {
        long retryBackoffMs = debugRuntimeConfigService.getLlmRetryBackoffMs();
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
