package ai.local.nalbbun.category.common;

import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;

import org.springframework.stereotype.Component;

@Component
public class CategoryResolver {

    private final RuleBasedCategoryResolver ruleBasedResolver;
    private final LlmCategoryResolver llmCategoryResolver;
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    public CategoryResolver(
            RuleBasedCategoryResolver ruleBasedResolver,
            LlmCategoryResolver llmCategoryResolver,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.ruleBasedResolver = ruleBasedResolver;
        this.llmCategoryResolver = llmCategoryResolver;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    public CategoryResolution resolve(String userQuery, ChatCategory requestedCategory) {
        if (requestedCategory != null) {
            return new CategoryResolution(requestedCategory, 100, "REQUEST_PARAM", "requested explicitly");
        }

        return switch (debugRuntimeConfigService.getResolverMode()) {
            case RULE -> ruleBasedResolver.resolve(userQuery);
            case LLM -> llmCategoryResolver.resolve(userQuery);
            case HYBRID -> resolveHybrid(userQuery);
        };
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