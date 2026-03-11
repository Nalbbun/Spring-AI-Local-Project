package ai.local.nalbbun.category.general.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedGeneralParser implements CategoryParsingStrategy<GeneralContext> {

    @Override
    public GeneralContext parse(ConversationState state, GeneralContext context) {
        context.setIntent("general_qa");
        return context;
    }

    @Override
    public String mode() {
        return "RULE";
    }
}