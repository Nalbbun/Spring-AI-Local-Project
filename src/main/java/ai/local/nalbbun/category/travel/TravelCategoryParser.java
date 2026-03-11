package ai.local.nalbbun.category.travel;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.category.travel.parser.LlmTravelParser;
import ai.local.nalbbun.category.travel.parser.RuleBasedTravelParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

@Component
public class TravelCategoryParser
        extends AbstractHybridCategoryParser<TravelContext>
        implements CategoryParser<TravelContext> {

    public TravelCategoryParser(
            RuleBasedTravelParser ruleBasedTravelParser,
            LlmTravelParser llmTravelParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedTravelParser, llmTravelParser, debugRuntimeConfigService);
    }

    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    @Override
    public TravelContext parse(ConversationState state) {
        return super.parse(state);
    }

    @Override
    protected TravelContext newContext() {
        return new TravelContext();
    }

    @Override
    protected boolean needsLlmAssist(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getDestination() == null
                || context.getDays() == null
                || context.getMaxBudget() == null
                || userQuery.contains("적당히")
                || userQuery.contains("알아서")
                || userQuery.contains("무난하게")
                || userQuery.contains("커플")
                || userQuery.contains("가족")
                || userQuery.contains("부모님")
                || userQuery.contains("아이와");
    }

    @Override
    protected void applyDefaults(TravelContext context, ConversationState state) {
        if (context.getDestination() == null || context.getDestination().isBlank()) {
            context.setDestination("제주도");
        }
        if (context.getDays() == null || context.getDays() <= 0) {
            context.setDays(2);
        }
        if (context.getMaxBudget() == null || context.getMaxBudget() <= 0) {
            context.setMaxBudget(500000);
        }
    }

    @Override
    protected void markMode(TravelContext context, String mode) {
        context.setParserMode(mode);
    }
}