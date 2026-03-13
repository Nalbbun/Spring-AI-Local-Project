package ai.local.nalbbun.category.common.parser;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

public interface CategoryParser<T extends CategoryContext> {
    ChatCategory category();
    T parse(ConversationState state);
}