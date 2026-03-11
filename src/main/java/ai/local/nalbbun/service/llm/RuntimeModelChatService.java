package ai.local.nalbbun.service.llm;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

@Service
public class RuntimeModelChatService {

    private final ChatClient.Builder openaiBuilder;
    private final ChatClient.Builder ollamaBuilder;
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;

    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest",
            "deepseek-r1:14b",
            "exaone3.5:7.8b",
            "gemma2:9b"
    ));

    public RuntimeModelChatService(
            @Qualifier("openaiBuilder") ChatClient.Builder openaiBuilder,
            @Qualifier("ollamaBuilder") ChatClient.Builder ollamaBuilder,
            DebugRuntimeModelConfigService debugRuntimeModelConfigService
    ) {
        this.openaiBuilder = openaiBuilder;
        this.ollamaBuilder = ollamaBuilder;
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
    }

    public String callText(RuntimeModelTarget target, String systemPrompt, String userPrompt) {
        ResolvedRuntimeModel resolved = resolve(target, false);

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
    }

    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            Class<T> responseType) {
        ResolvedRuntimeModel resolved = resolve(target, false);

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
    }

    public <T> T callEntity(RuntimeModelTarget target,
                            String systemPrompt,
                            String userPrompt,
                            ParameterizedTypeReference<T> responseType) {
        ResolvedRuntimeModel resolved = resolve(target, false);

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
    }

    public <T> T callEntityWithTools(RuntimeModelTarget target,
                                     String systemPrompt,
                                     String userPrompt,
                                     Object toolObject,
                                     ParameterizedTypeReference<T> responseType) {
        ResolvedRuntimeModel resolved = resolve(target, true);

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
    }

    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        ResolvedRuntimeModel resolved = resolve(target, requiresTools);
        return resolved.ollama()
                ? "OLLAMA:" + resolved.modelName()
                : "OPENAI:default";
    }

    private ResolvedRuntimeModel resolve(RuntimeModelTarget target, boolean requiresTools) {
        String configuredModel = getConfiguredModel(target);

        if (configuredModel == null || configuredModel.isBlank()) {
            return new ResolvedRuntimeModel(false, null);
        }

        if (requiresTools && !supportsTools(configuredModel)) {
            return new ResolvedRuntimeModel(false, null);
        }

        return new ResolvedRuntimeModel(true, configuredModel);
    }

    private String getConfiguredModel(RuntimeModelTarget target) {
        return switch (target) {
            case GENERAL -> debugRuntimeModelConfigService.getGeneralModel();
            case DEV -> debugRuntimeModelConfigService.getDevModel();
            case MICE -> debugRuntimeModelConfigService.getMiceModel();
            case TRAVEL_SEARCH -> debugRuntimeModelConfigService.getTravelSearchModel();
            case TRAVEL_PLAN -> debugRuntimeModelConfigService.getTravelPlanModel();
        };
    }

    private boolean supportsTools(String modelName) {
        String normalized = modelName == null ? "" : modelName.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return false;
        }

        if (normalized.contains("blossom")) {
            return false;
        }

        return toolCapableOllamaModels.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(modelName));
    }

    private record ResolvedRuntimeModel(boolean ollama, String modelName) {
    }
}