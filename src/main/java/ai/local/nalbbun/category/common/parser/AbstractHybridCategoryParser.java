package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

/**
 * AbstractHybridCategoryParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: abstract hybrid category parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public abstract class AbstractHybridCategoryParser<T extends CategoryContext> {

    /** ruleStrategy 값을 보관한다. */
    private final CategoryParsingStrategy<T> ruleStrategy;
    /** llmStrategy 값을 보관한다. */
    private final CategoryParsingStrategy<T> llmStrategy;
    /** debugRuntimeConfigService 값을 보관한다. */
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleStrategy ruleStrategy 값
     * @param llmStrategy llmStrategy 값
     * @param debugRuntimeConfigService debugRuntimeConfigService 값
     */
    protected AbstractHybridCategoryParser(
            CategoryParsingStrategy<T> ruleStrategy,
            CategoryParsingStrategy<T> llmStrategy,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.ruleStrategy = ruleStrategy;
        this.llmStrategy = llmStrategy;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @return T 타입의 처리 결과
     */
    public T parse(ConversationState state) {
        T context = newContext();
        CategoryParserMode mode = debugRuntimeConfigService.getParserMode(category());

        switch (mode) {
            case RULE -> {
                context = ruleStrategy.parse(state, context);
                applyDefaults(context, state);
                markMode(context, "RULE");
                return context;
            }
            case LLM -> {
                context = llmStrategy.parse(state, context);
                applyDefaults(context, state);
                markMode(context, "LLM");
                return context;
            }
            case HYBRID -> {
                context = ruleStrategy.parse(state, context);

                if (needsLlmAssist(state, context)) {
                    context = llmStrategy.parse(state, context);
                    applyDefaults(context, state);
                    markMode(context, "HYBRID(RULE->LLM)");
                } else {
                    applyDefaults(context, state);
                    markMode(context, "HYBRID(RULE)");
                }
                return context;
            }
            default -> throw new IllegalStateException("Unsupported parser mode: " + mode);
        }
    }

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    protected abstract ChatCategory category();

    /**
     * newContext 기능을 수행한다.
     * @return T 타입의 처리 결과
     */
    protected abstract T newContext();

    /**
     * needsLlmAssist 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    protected abstract boolean needsLlmAssist(ConversationState state, T context);

    /**
     * applyDefaults 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param state 현재 처리 상태 정보
     */
    protected abstract void applyDefaults(T context, ConversationState state);

    /**
     * markMode 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param mode mode 값
     */
    protected abstract void markMode(T context, String mode);
}