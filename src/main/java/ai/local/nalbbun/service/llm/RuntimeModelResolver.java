package ai.local.nalbbun.service.llm;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

@Component
public class RuntimeModelResolver {

    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final ExternalLlmFallbackPolicy fallbackPolicy;
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest",
            "deepseek-r1:14b",
            "exaone3.5:7.8b",
            "gemma2:9b"
    ));

    public RuntimeModelResolver(
            DebugRuntimeModelConfigService debugRuntimeModelConfigService,
            @Value("${app.llm.fallback-policy:ALLOW_OPENAI}") String fallbackPolicy
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.fallbackPolicy = ExternalLlmFallbackPolicy.from(fallbackPolicy);
    }

    public RuntimeModelSelection resolve(RuntimeModelTarget target, boolean requiresTools) {
        String configuredModel = getConfiguredModel(target);

        if (configuredModel == null || configuredModel.isBlank()) {
            return fallbackOrFail(target, "no-local-model-configured");
        }

        if (requiresTools && !supportsTools(configuredModel)) {
            return fallbackOrFail(target, "local-model-does-not-support-tools");
        }

        return new RuntimeModelSelection(true, configuredModel, false, "local-model-selected");
    }

    private RuntimeModelSelection fallbackOrFail(RuntimeModelTarget target, String reason) {
        if (fallbackPolicy == ExternalLlmFallbackPolicy.ALLOW_OPENAI) {
            return new RuntimeModelSelection(false, null, true, reason);
        }

        throw new RuntimeModelResolutionException(
                "외부 전송 차단 정책으로 인해 OpenAI fallback이 허용되지 않습니다. target=%s, reason=%s"
                        .formatted(target, reason)
        );
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
}
