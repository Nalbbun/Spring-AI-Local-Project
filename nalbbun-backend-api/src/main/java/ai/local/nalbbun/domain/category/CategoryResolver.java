package ai.local.nalbbun.domain.category;

import ai.local.nalbbun.domain.runtime.port.RuntimeCategoryPolicyPort;
import ai.local.nalbbun.domain.category.model.CategoryResolution;
import ai.local.nalbbun.domain.category.model.ChatCategory;

import org.springframework.stereotype.Component;

/**
 * Category Resolver 타입이다.
 *
 * <p>기능 설명: 입력 조건을 해석해 적절한 선택 결과를 도출한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class CategoryResolver {

    private final RuleBasedCategoryResolver ruleBasedResolver;
    private final LlmCategoryResolver llmCategoryResolver;
    private final RuntimeCategoryPolicyPort debugRuntimeConfigService;

    /**
     * Category Resolver 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public CategoryResolver(
            RuleBasedCategoryResolver ruleBasedResolver,
            LlmCategoryResolver llmCategoryResolver,
            RuntimeCategoryPolicyPort debugRuntimeConfigService
    ) {
        this.ruleBasedResolver = ruleBasedResolver;
        this.llmCategoryResolver = llmCategoryResolver;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    /**
     * resolve 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * resolve Hybrid 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * Mixed Intent 여부를 판별한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * contains Any 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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