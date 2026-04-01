package ai.local.nalbbun.domain.runtime.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.domain.runtime.port.RuntimeModelCatalogPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeOllamaConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeOpenAiConnectionPort;
import ai.local.nalbbun.domain.runtime.port.RuntimeVllmConnectionPort;
import ai.local.nalbbun.domain.runtime.model.ModelPriority;
import ai.local.nalbbun.domain.runtime.model.RuntimeLlmProvider;
import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;

/**
 * 카테고리별 모델 우선순위(ModelPriority)를 반영한 런타임 모델 결정 컴포넌트.
 */
@Component
public class RuntimeModelResolver {

    private final RuntimeModelCatalogPort debugRuntimeModelConfigService;
    private final RuntimeOllamaConnectionPort ollamaConnectionService;
    private final RuntimeVllmConnectionPort vllmConnectionService;
    private final RuntimeOpenAiConnectionPort openAiConnectionService;
    private final CategoryModelPriorityService priorityService;
    private final ExternalLlmFallbackPolicy globalFallbackPolicy;
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest"
    ));

    public RuntimeModelResolver(
            RuntimeModelCatalogPort debugRuntimeModelConfigService,
            RuntimeOllamaConnectionPort ollamaConnectionService,
            RuntimeVllmConnectionPort vllmConnectionService,
            RuntimeOpenAiConnectionPort openAiConnectionService,
            CategoryModelPriorityService priorityService,
            @Value("${app.llm.fallback-policy:ALLOW_OPENAI}") String fallbackPolicy
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.ollamaConnectionService = ollamaConnectionService;
        this.vllmConnectionService = vllmConnectionService;
        this.openAiConnectionService = openAiConnectionService;
        this.priorityService = priorityService;
        this.globalFallbackPolicy = ExternalLlmFallbackPolicy.from(fallbackPolicy);
    }

    public RuntimeModelSelection resolve(RuntimeModelTarget target, boolean requiresTools) {
        ModelPriority priority = priorityService.get(target);

        return switch (priority) {
            case OLLAMA_ONLY -> resolveOllamaOnly(target, requiresTools);
            case VLLM_ONLY -> resolveVllmOnly(target, requiresTools);
            case OPENAI_ONLY -> resolveOpenAiOnly(target);
            case OPENAI_FIRST -> resolveOpenAiFirst(target, requiresTools);
            case VLLM_FIRST -> resolveVllmFirst(target, requiresTools);
            default -> resolveOllamaFirst(target, requiresTools);
        };
    }

    private RuntimeModelSelection resolveOllamaFirst(RuntimeModelTarget target, boolean requiresTools) {
        String model = getConfiguredOllamaModel(target);
        if (hasText(model)) {
            if (!requiresTools || supportsTools(model)) {
                return new RuntimeModelSelection(
                        RuntimeLlmProvider.OLLAMA,
                        model,
                        ollamaConnectionService.getBaseUrl(),
                        null,
                        false,
                        "ollama-first:local-selected"
                );
            }
            return fallbackOrFail(target, "ollama-first:tool-not-supported");
        }
        return fallbackOrFail(target, "ollama-first:no-local-model");
    }

    private RuntimeModelSelection resolveVllmFirst(RuntimeModelTarget target, boolean requiresTools) {
        if (requiresTools) {
            return fallbackOrFail(target, "vllm-first:tools-not-wired");
        }
        if (hasText(vllmConnectionService.getBaseUrl())) {
            return new RuntimeModelSelection(
                    RuntimeLlmProvider.VLLM,
                    configuredVllmModel(target),
                    vllmConnectionService.getBaseUrl(),
                    vllmConnectionService.getKeyProvider(),
                    false,
                    "vllm-first:selected"
            );
        }
        return fallbackOrFail(target, "vllm-first:not-configured");
    }

    private RuntimeModelSelection resolveOllamaOnly(RuntimeModelTarget target, boolean requiresTools) {
        String model = getConfiguredOllamaModel(target);
        if (!hasText(model)) {
            throw new RuntimeModelResolutionException("OLLAMA_ONLY 정책: Ollama 모델이 설정되지 않았습니다. target=" + target);
        }
        if (requiresTools && !supportsTools(model)) {
            throw new RuntimeModelResolutionException("OLLAMA_ONLY 정책: tool calling 미지원 모델입니다. target=" + target + ", model=" + model);
        }
        return new RuntimeModelSelection(
                RuntimeLlmProvider.OLLAMA,
                model,
                ollamaConnectionService.getBaseUrl(),
                null,
                false,
                "ollama-only:selected"
        );
    }

    private RuntimeModelSelection resolveVllmOnly(RuntimeModelTarget target, boolean requiresTools) {
        if (requiresTools) {
            throw new RuntimeModelResolutionException("VLLM_ONLY 정책: tools 호출은 아직 Ollama 전용입니다. target=" + target);
        }
        if (!hasText(vllmConnectionService.getBaseUrl())) {
            throw new RuntimeModelResolutionException("VLLM_ONLY 정책: vLLM base-url 이 설정되지 않았습니다. target=" + target);
        }
        return new RuntimeModelSelection(
                RuntimeLlmProvider.VLLM,
                configuredVllmModel(target),
                vllmConnectionService.getBaseUrl(),
                vllmConnectionService.getKeyProvider(),
                false,
                "vllm-only:selected"
        );
    }

    private RuntimeModelSelection resolveOpenAiFirst(RuntimeModelTarget target, boolean requiresTools) {
        if (hasText(openAiConnectionService.getBaseUrl())) {
            return new RuntimeModelSelection(
                    RuntimeLlmProvider.OPENAI,
                    configuredOpenAiModel(target),
                    openAiConnectionService.getBaseUrl(),
                    openAiConnectionService.getKeyProvider(),
                    false,
                    "openai-first:selected"
            );
        }
        return requiresTools ? resolveOllamaFirst(target, true) : fallbackOrFail(target, "openai-first:not-configured");
    }

    private RuntimeModelSelection resolveOpenAiOnly(RuntimeModelTarget target) {
        return new RuntimeModelSelection(
                RuntimeLlmProvider.OPENAI,
                configuredOpenAiModel(target),
                openAiConnectionService.getBaseUrl(),
                openAiConnectionService.getKeyProvider(),
                false,
                "openai-only:selected"
        );
    }

    private RuntimeModelSelection fallbackOrFail(RuntimeModelTarget target, String reason) {
        if (globalFallbackPolicy == ExternalLlmFallbackPolicy.ALLOW_OPENAI) {
            return new RuntimeModelSelection(
                    RuntimeLlmProvider.OPENAI,
                    configuredOpenAiModel(target),
                    openAiConnectionService.getBaseUrl(),
                    openAiConnectionService.getKeyProvider(),
                    true,
                    reason
            );
        }
        throw new RuntimeModelResolutionException(
                "외부 전송 차단 정책으로 fallback 불가. target=%s, reason=%s".formatted(target, reason));
    }

    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        try {
            RuntimeModelSelection sel = resolve(target, requiresTools);
            ModelPriority p = priorityService.get(target);
            return "provider=" + sel.provider().name()
                    + ", model=" + (sel.modelName() != null ? sel.modelName() : "default")
                    + ", priority=" + p.name()
                    + ", reason=" + sel.reason();
        } catch (Exception e) {
            return "resolve-failed:" + e.getMessage();
        }
    }

    private String getConfiguredOllamaModel(RuntimeModelTarget target) {
        return switch (target) {
            case GENERAL -> debugRuntimeModelConfigService.getGeneralModel();
            case DEV -> debugRuntimeModelConfigService.getDevModel();
            case MICE -> debugRuntimeModelConfigService.getMiceModel();
            case TRAVEL_SEARCH -> debugRuntimeModelConfigService.getTravelSearchModel();
            case TRAVEL_PLAN -> debugRuntimeModelConfigService.getTravelPlanModel();
        };
    }

    private String configuredVllmModel(RuntimeModelTarget target) {
        String model = vllmConnectionService.getConfiguredOrDefaultModel();
        return hasText(model) ? model : configuredOpenAiModel(target);
    }

    private String configuredOpenAiModel(RuntimeModelTarget target) {
        String model = openAiConnectionService.getConfiguredOrDefaultModel();
        if (hasText(model)) return model;
        return "gpt-4.1-mini";
    }

    private boolean supportsTools(String modelName) {
        if (modelName == null || modelName.isBlank()) return false;
        String n = modelName.trim().toLowerCase(Locale.ROOT);
        if (n.contains("blossom")) return false;
        return toolCapableOllamaModels.stream().anyMatch(a -> a.equalsIgnoreCase(modelName));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
