package ai.local.nalbbun.service.llm;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

@Component
public class RuntimeModelResolver {

    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final DebugRuntimeConfigService debugRuntimeConfigService;
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest"
    ));

    public RuntimeModelResolver(
            DebugRuntimeModelConfigService debugRuntimeModelConfigService,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
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
        if (debugRuntimeConfigService.getFallbackPolicyEnum() == ExternalLlmFallbackPolicy.ALLOW_OPENAI) {
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
