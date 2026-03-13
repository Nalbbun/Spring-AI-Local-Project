package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;

public interface CategoryMemoryRule<T extends CategoryContext> {

    ChatCategory category();

    CategoryMemoryUpdate extract(ConversationState state, T context, String assistantResponse);
}