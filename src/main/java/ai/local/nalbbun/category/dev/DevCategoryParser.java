package ai.local.nalbbun.category.dev;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.common.parser.AbstractHybridCategoryParser;
import ai.local.nalbbun.category.common.parser.CategoryParser;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.category.dev.parser.LlmDevParser;
import ai.local.nalbbun.category.dev.parser.RuleBasedDevParser;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;

@Component
public class DevCategoryParser
        extends AbstractHybridCategoryParser<DevContext>
        implements CategoryParser<DevContext> {

    public DevCategoryParser(
            RuleBasedDevParser ruleBasedDevParser,
            LlmDevParser llmDevParser,
            DebugRuntimeConfigService debugRuntimeConfigService
    ) {
        super(ruleBasedDevParser, llmDevParser, debugRuntimeConfigService);
    }

    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    @Override
    public DevContext parse(ConversationState state) {
        return super.parse(state);
    }

    @Override
    protected DevContext newContext() {
        return new DevContext();
    }

    @Override
    protected boolean needsLlmAssist(ConversationState state, DevContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        return context.getTaskType() == null
                || context.getTopic() == null
                || q.contains("전반적으로")
                || q.contains("정리해줘")
                || q.contains("어떻게 할까")
                || q.contains("설계부터")
                || q.contains("구현 방향");
    }

    @Override
    protected void applyDefaults(DevContext context, ConversationState state) {
        if (context.getTaskType() == null || context.getTaskType().isBlank()) {
            context.setTaskType("implementation");
        }
        if (context.getTopic() == null || context.getTopic().isBlank()) {
            context.setTopic("general-dev");
        }
    }

    @Override
    protected void markMode(DevContext context, String mode) {
        context.setParserMode(mode);
    }
}