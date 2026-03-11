package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface CategoryHandler {

    ChatCategory category();

    default boolean supports(ChatCategory category) {
        return this.category() == category;
    }

    CategoryResult handle(ConversationState state, SseEmitter emitter);
}