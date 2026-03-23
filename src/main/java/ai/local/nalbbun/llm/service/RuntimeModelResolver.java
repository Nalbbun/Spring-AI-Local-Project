package ai.local.nalbbun.llm.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.internal.model.RuntimeModelTarget;
import ai.local.nalbbun.internal.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.llm.model.ModelPriority;

/**
 * 카테고리별 모델 우선순위(ModelPriority)를 반영한 런타임 모델 결정 컴포넌트.
 *
 * OLLAMA_FIRST : Ollama 설정 모델 시도 → 없으면 OpenAI fallback
 * OPENAI_FIRST : OpenAI 바로 사용 (Ollama 무시)
 * OLLAMA_ONLY  : Ollama 전용 — Ollama 모델이 없으면 예외
 * OPENAI_ONLY  : OpenAI 전용
 */
@Component
public class RuntimeModelResolver {

    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final CategoryModelPriorityService   priorityService;
    private final ExternalLlmFallbackPolicy      globalFallbackPolicy;
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest"
    ));

    public RuntimeModelResolver(
            DebugRuntimeModelConfigService debugRuntimeModelConfigService,
            CategoryModelPriorityService priorityService,
            @Value("${app.llm.fallback-policy:ALLOW_OPENAI}") String fallbackPolicy
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.priorityService    = priorityService;
        this.globalFallbackPolicy = ExternalLlmFallbackPolicy.from(fallbackPolicy);
    }

    public RuntimeModelSelection resolve(RuntimeModelTarget target, boolean requiresTools) {
        ModelPriority priority = priorityService.get(target);

        return switch (priority) {
            case OLLAMA_ONLY   -> resolveOllamaOnly(target, requiresTools);
            case OPENAI_ONLY   -> resolveOpenAiOnly(target);
            case OPENAI_FIRST  -> resolveOpenAiFirst(target, requiresTools);
            default            -> resolveOllamaFirst(target, requiresTools);  // OLLAMA_FIRST
        };
    }

    // ── 우선순위별 결정 로직 ──────────────────────────────────

    /** OLLAMA_FIRST: Ollama 시도 → 없으면 OpenAI */
    private RuntimeModelSelection resolveOllamaFirst(RuntimeModelTarget target, boolean requiresTools) {
        String model = getConfiguredModel(target);
        if (model != null && !model.isBlank()) {
            if (!requiresTools || supportsTools(model)) {
                return new RuntimeModelSelection(true, model, false, "ollama-first:local-selected");
            }
            return fallbackOrFail(target, "ollama-first:tool-not-supported");
        }
        return fallbackOrFail(target, "ollama-first:no-local-model");
    }

    /** OLLAMA_ONLY: Ollama 전용 — 없으면 예외 */
    private RuntimeModelSelection resolveOllamaOnly(RuntimeModelTarget target, boolean requiresTools) {
        String model = getConfiguredModel(target);
        if (model == null || model.isBlank()) {
            throw new RuntimeModelResolutionException(
                "OLLAMA_ONLY 정책: Ollama 모델이 설정되지 않았습니다. target=" + target);
        }
        if (requiresTools && !supportsTools(model)) {
            throw new RuntimeModelResolutionException(
                "OLLAMA_ONLY 정책: tool calling 미지원 모델입니다. target=" + target + ", model=" + model);
        }
        return new RuntimeModelSelection(true, model, false, "ollama-only:selected");
    }

    /** OPENAI_FIRST: OpenAI 바로 사용 */
    private RuntimeModelSelection resolveOpenAiFirst(RuntimeModelTarget target, boolean requiresTools) {
        return new RuntimeModelSelection(false, null, false, "openai-first:selected");
    }

    /** OPENAI_ONLY: OpenAI 전용 */
    private RuntimeModelSelection resolveOpenAiOnly(RuntimeModelTarget target) {
        return new RuntimeModelSelection(false, null, false, "openai-only:selected");
    }

    // ── 기존 fallback 로직 유지 ───────────────────────────────
    private RuntimeModelSelection fallbackOrFail(RuntimeModelTarget target, String reason) {
        if (globalFallbackPolicy == ExternalLlmFallbackPolicy.ALLOW_OPENAI) {
            return new RuntimeModelSelection(false, null, true, reason);
        }
        throw new RuntimeModelResolutionException(
            "외부 전송 차단 정책으로 OpenAI fallback 불가. target=%s, reason=%s"
                .formatted(target, reason));
    }

    /** describe용 — 설정/디버그 메시지 생성 */
    public String describeResolvedModel(RuntimeModelTarget target, boolean requiresTools) {
        try {
            RuntimeModelSelection sel = resolve(target, requiresTools);
            ModelPriority p = priorityService.get(target);
            return "provider=" + (sel.ollama() ? "OLLAMA" : "OPENAI")
                + ", model=" + (sel.modelName() != null ? sel.modelName() : "default")
                + ", priority=" + p.name()
                + ", reason=" + sel.reason();
        } catch (Exception e) {
            return "resolve-failed:" + e.getMessage();
        }
    }

    private String getConfiguredModel(RuntimeModelTarget target) {
        return switch (target) {
            case GENERAL       -> debugRuntimeModelConfigService.getGeneralModel();
            case DEV           -> debugRuntimeModelConfigService.getDevModel();
            case MICE          -> debugRuntimeModelConfigService.getMiceModel();
            case TRAVEL_SEARCH -> debugRuntimeModelConfigService.getTravelSearchModel();
            case TRAVEL_PLAN   -> debugRuntimeModelConfigService.getTravelPlanModel();
        };
    }

    private boolean supportsTools(String modelName) {
        if (modelName == null || modelName.isBlank()) return false;
        String n = modelName.trim().toLowerCase(Locale.ROOT);
        if (n.contains("blossom")) return false;
        return toolCapableOllamaModels.stream()
                .anyMatch(a -> a.equalsIgnoreCase(modelName));
    }
}
