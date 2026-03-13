package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

public abstract class AbstractHybridCategoryParser<T extends CategoryContext> {

    private final CategoryParsingStrategy<T> ruleStrategy;
    private final CategoryParsingStrategy<T> llmStrategy;
    private final DebugRuntimeConfigService debugRuntimeConfigService;

    protected AbstractHybridCategoryParser(
            CategoryParsingStrategy<T> ruleStrategy,
            CategoryParsingStrategy<T> llmStrategy,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        this.ruleStrategy = ruleStrategy;
        this.llmStrategy = llmStrategy;
        this.debugRuntimeConfigService = debugRuntimeConfigService;
    }

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

    protected abstract ChatCategory category();

    protected abstract T newContext();

    protected abstract boolean needsLlmAssist(ConversationState state, T context);

    protected abstract void applyDefaults(T context, ConversationState state);

    protected abstract void markMode(T context, String mode);
}