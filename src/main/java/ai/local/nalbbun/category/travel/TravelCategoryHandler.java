package ai.local.nalbbun.category.travel;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdateResult;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdater;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class TravelCategoryHandler implements CategoryHandler {

    private final TravelCategoryParser parser;
    private final TravelWorkflow travelWorkflow;
    private final ConversationMemoryService memoryService;
    private final CategoryMemoryUpdater categoryMemoryUpdater;
    private final AgentEventPublisher agentEventPublisher;

    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    @Override
    public CategoryResult handle(ConversationState state, SseEmitter emitter) {
        TravelContext context = parser.parse(state);
        state.setCategoryContext(context);

        String response = travelWorkflow.execute(state, context, emitter);

        memoryService.addAssistantMessage(state.getConversationId(), ChatCategory.TRAVEL, response);

        CategoryMemoryUpdateResult memoryResult = categoryMemoryUpdater.update(state, response);
        agentEventPublisher.send(
                emitter,
                "MemoryUpdater",
                memoryResult.addedNoteCount() > 0 || memoryResult.isSummaryUpdated() ? "updated" : "skipped",
                memoryResult.toDebugMessage()
        );

        return CategoryResult.builder()
                .finalResponse(response)
                .payload(context)
                .build();
    }
}