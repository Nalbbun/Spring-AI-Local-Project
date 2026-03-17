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
 * Runtime Model Resolver 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Component
public class RuntimeModelResolver {

    private final DebugRuntimeModelConfigService debugRuntimeModelConfigService;
    private final ExternalLlmFallbackPolicy fallbackPolicy;
    private final Set<String> toolCapableOllamaModels = new HashSet<>(List.of(
            "qwen2.5-coder:14b",
            "qwen3-coder:latest"
    ));

    /**
     * Runtime Model Resolver 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public RuntimeModelResolver(
            DebugRuntimeModelConfigService debugRuntimeModelConfigService,
            @Value("${app.llm.fallback-policy:ALLOW_OPENAI}") String fallbackPolicy
    ) {
        this.debugRuntimeModelConfigService = debugRuntimeModelConfigService;
        this.fallbackPolicy = ExternalLlmFallbackPolicy.from(fallbackPolicy);
    }

    /**
     * resolve 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * fallback Or Fail 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * Configured Model 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * supports Tools 가능 여부를 확인한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
