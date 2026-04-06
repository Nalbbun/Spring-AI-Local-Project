package ai.local.nalbbun.domain.category;

import ai.local.nalbbun.domain.runtime.port.RuntimeCategoryPolicyPort;
import ai.local.nalbbun.domain.category.model.CategoryResolution;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ExecutionMode;

import org.springframework.stereotype.Component;

/**
 * Category Resolver 타입이다.
 */
@Component
public class CategoryResolver {

    private final RuleBasedCategoryResolver ruleBasedResolver;
    private final LlmCategoryResolver llmCategoryResolver;
    private final RuntimeCategoryPolicyPort debugRuntimeConfigService;

    public CategoryResolver(
            RuleBasedCategoryResolver ruleBasedResolver,
            LlmCategoryResolver llmCategoryResolver,
            RuntimeCategoryPolicyPort debugRuntimeConfigService
    ) {
        this.ruleBasedResolver = ruleBasedResolver;
        this.llmCategoryResolver = llmCategoryResolver;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    public CategoryResolution resolve(String userQuery, ChatCategory requestedCategory, ExecutionMode requestedExecutionMode) {
        if (requestedCategory != null) {
            ExecutionMode executionMode = normalizeExecutionMode(requestedCategory, requestedExecutionMode);
            return new CategoryResolution(requestedCategory, 100, "REQUEST_PARAM", "requested explicitly", executionMode);
        }

        CategoryResolution resolution = switch (debugRuntimeConfigService.getResolverMode()) {
            case RULE -> ruleBasedResolver.resolve(userQuery);
            case LLM -> llmCategoryResolver.resolve(userQuery);
            case HYBRID -> resolveHybrid(userQuery);
        };
        resolution.setExecutionMode(normalizeExecutionMode(resolution.getCategory(), requestedExecutionMode));
        return resolution;
    }

    private CategoryResolution resolveHybrid(String userQuery) {
        CategoryResolution ruleResult = ruleBasedResolver.resolve(userQuery);

        boolean needLlm =
                ruleResult.getConfidence() < 80 ||
                isMixedIntent(userQuery);

        if (!needLlm) {
            ruleResult.setResolverMode("HYBRID(RULE)");
            return ruleResult;
        }

        CategoryResolution llmResult = llmCategoryResolver.resolve(userQuery);
        llmResult.setResolverMode("HYBRID(LLM)");
        return llmResult;
    }

    private ExecutionMode normalizeExecutionMode(ChatCategory category, ExecutionMode requestedExecutionMode) {
        if (requestedExecutionMode != null && requestedExecutionMode != ExecutionMode.AUTO) {
            return requestedExecutionMode;
        }
        return debugRuntimeConfigService.getDefaultExecutionMode(category);
    }

    private boolean isMixedIntent(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return true;
        }

        String q = userQuery.toLowerCase();
        boolean hasTravel = containsAny(q, "여행", "숙소", "맛집", "관광", "일정");
        boolean hasDev = containsAny(q, "개발", "스프링", "자바", "도커", "쿠버네티스", "리팩토링");
        boolean hasMice = containsAny(q, "행사", "포럼", "컨퍼런스", "제안서", "운영", "mice");

        int count = 0;
        if (hasTravel) count++;
        if (hasDev) count++;
        if (hasMice) count++;

        return count >= 2;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
