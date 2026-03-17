package ai.local.nalbbun.service.llm;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

/**
 * RuntimeModelResolver는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: runtime model resolver 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuntimeModelResolver {

    /** debugRuntimeModelConfigService 값을 보관한다. */
    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    /** fallbackPolicy 값을 보관한다. */
    private final ExternalLlmFallbackPolicy fallbackPolicy;
    /** toolCapableOllamaModels 값을 보관한다. */
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest"
    ));

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param debugRuntimeModelConfigService debugRuntimeModelConfigService 값
     * @param fallbackPolicy fallbackPolicy 값
     */
    public RuntimeModelResolver(
            DebugRuntimeModelConfigService debugRuntimeModelConfigService,
            @Value("${app.llm.fallback-policy:ALLOW_OPENAI}") String fallbackPolicy
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.fallbackPolicy = ExternalLlmFallbackPolicy.from(fallbackPolicy);
    }

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param target target 값
     * @param requiresTools requiresTools 값
     * @return RuntimeModelSelection 타입의 처리 결과
     */
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

    /**
     * fallbackOrFail 기능을 수행한다.
     *
     * @param target target 값
     * @param reason reason 값
     * @return RuntimeModelSelection 타입의 처리 결과
     */
    private RuntimeModelSelection fallbackOrFail(RuntimeModelTarget target, String reason) {
        if (fallbackPolicy == ExternalLlmFallbackPolicy.ALLOW_OPENAI) {
            return new RuntimeModelSelection(false, null, true, reason);
        }

        throw new RuntimeModelResolutionException(
                "외부 전송 차단 정책으로 인해 OpenAI fallback이 허용되지 않습니다. target=%s, reason=%s"
                        .formatted(target, reason)
        );
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param target target 값
     * @return 처리 결과 문자열
     */
    private String getConfiguredModel(RuntimeModelTarget target) {
        return switch (target) {
            case GENERAL -> debugRuntimeModelConfigService.getGeneralModel();
            case DEV -> debugRuntimeModelConfigService.getDevModel();
            case MICE -> debugRuntimeModelConfigService.getMiceModel();
            case TRAVEL_SEARCH -> debugRuntimeModelConfigService.getTravelSearchModel();
            case TRAVEL_PLAN -> debugRuntimeModelConfigService.getTravelPlanModel();
        };
    }

    /**
     * 지원 여부를 확인한다.
     *
     * @param modelName modelName 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
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
