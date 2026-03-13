package ai.local.nalbbun.category.general;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.category.general.parser.LlmGeneralParser;
import ai.local.nalbbun.category.general.parser.RuleBasedGeneralParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

@Component
public class GeneralCategoryParser
        extends AbstractHybridCategoryParser<GeneralContext>
        implements CategoryParser<GeneralContext> {

    public GeneralCategoryParser(
            RuleBasedGeneralParser ruleBasedGeneralParser,
            LlmGeneralParser llmGeneralParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedGeneralParser, llmGeneralParser, debugRuntimeConfigService);
    }

    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    @Override
    public GeneralContext parse(ConversationState state) {
        return super.parse(state);
    }

    @Override
    protected GeneralContext newContext() {
        return new GeneralContext();
    }

    @Override
    protected boolean needsLlmAssist(ConversationState state, GeneralContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();
        return q.length() > 20 || q.contains("조금 더") || q.contains("다시") || q.contains("요약");
    }

    @Override
    protected void applyDefaults(GeneralContext context, ConversationState state) {
        if (context.getIntent() == null || context.getIntent().isBlank()) {
            context.setIntent("general_qa");
        }
    }

    @Override
    protected void markMode(GeneralContext context, String mode) {
        context.setParserMode(mode);
    }
}