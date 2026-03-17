package ai.local.nalbbun.category.common;

import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;

import org.springframework.stereotype.Component;

/**
 * CategoryResolver는 조건에 따라 적절한 대상이나 값을 해석하는 리졸버이다.
 * <p>주요 기능: category resolver 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class CategoryResolver {

    /** ruleBasedResolver 값을 보관한다. */
    private final RuleBasedCategoryResolver ruleBasedResolver;
    /** llmCategoryResolver 값을 보관한다. */
    private final LlmCategoryResolver llmCategoryResolver;
    /** debugRuntimeConfigService 값을 보관한다. */
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleBasedResolver ruleBasedResolver 값
     * @param llmCategoryResolver llmCategoryResolver 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    public CategoryResolver(
            RuleBasedCategoryResolver ruleBasedResolver,
            LlmCategoryResolver llmCategoryResolver,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.ruleBasedResolver = ruleBasedResolver;
        this.llmCategoryResolver = llmCategoryResolver;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @param requestedCategory requestedCategory 값
     * @return CategoryResolution 타입의 처리 결과
     */
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

    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return CategoryResolution 타입의 처리 결과
     */
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

    /**
     * 조건 충족 여부를 확인한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
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

    /**
     * containsAny 기능을 수행한다.
     *
     * @param source source 값
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}