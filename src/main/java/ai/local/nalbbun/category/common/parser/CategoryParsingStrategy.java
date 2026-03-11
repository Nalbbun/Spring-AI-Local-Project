package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

public interface CategoryParsingStrategy<T extends CategoryContext> {

    T parse(ConversationState state, T context);

    String mode();
}