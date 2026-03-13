package ai.local.nalbbun.category.mice;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.category.mice.parser.LlmMiceParser;
import ai.local.nalbbun.category.mice.parser.RuleBasedMiceParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

@Component
public class MiceCategoryParser
        extends AbstractHybridCategoryParser<MiceContext>
        implements CategoryParser<MiceContext> {

    public MiceCategoryParser(
            RuleBasedMiceParser ruleBasedMiceParser,
            LlmMiceParser llmMiceParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedMiceParser, llmMiceParser, debugRuntimeConfigService);
    }

    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    @Override
    public MiceContext parse(ConversationState state) {
        return super.parse(state);
    }

    @Override
    protected MiceContext newContext() {
        return new MiceContext();
    }

    @Override
    protected boolean needsLlmAssist(ConversationState state, MiceContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getEventType() == null
                || context.getDeliverableType() == null
                || context.getTargetRegion() == null
                || q.contains("방향성")
                || q.contains("전략")
                || q.contains("메시지")
                || q.contains("브랜딩");
    }

    @Override
    protected void applyDefaults(MiceContext context, ConversationState state) {
        if (context.getEventType() == null || context.getEventType().isBlank()) {
            context.setEventType("mice-event");
        }
        if (context.getDeliverableType() == null || context.getDeliverableType().isBlank()) {
            context.setDeliverableType("strategy");
        }
        if (context.getTargetRegion() == null || context.getTargetRegion().isBlank()) {
            context.setTargetRegion("global");
        }
    }

    @Override
    protected void markMode(MiceContext context, String mode) {
        context.setParserMode(mode);
    }
}