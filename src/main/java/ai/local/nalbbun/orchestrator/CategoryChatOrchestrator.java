package ai.local.nalbbun.orchestrator;

import ai.local.nalbbun.category.common.CategoryResolver;
import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.CategoryResult;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.registry.CategoryHandlerRegistry;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class CategoryChatOrchestrator {

    private final CategoryResolver categoryResolver;
    private final CategoryHandlerRegistry categoryHandlerRegistry;
    private final ConversationMemoryService conversationMemoryService;
    private final AgentEventPublisher agentEventPublisher;

    public CategoryResult execute(String userQuery,
                                  String conversationId,
                                  ChatCategory requestedCategory,
                                  SseEmitter emitter) {

        ConversationState state = new ConversationState();
        state.setConversationId(conversationId);
        state.setUserQuery(userQuery);
        state.setRequestedCategory(requestedCategory);

        CategoryResolution resolution = categoryResolver.resolve(userQuery, requestedCategory);
        state.setResolvedCategory(resolution.getCategory());
        state.getAttributes().put("resolverMode", resolution.getResolverMode());
        state.getAttributes().put("resolverReason", resolution.getReason());
        state.getAttributes().put("resolverConfidence", resolution.getConfidence());

        conversationMemoryService.addUserMessage(conversationId, resolution.getCategory(), userQuery);

        agentEventPublisher.send(
                emitter,
                "CategoryResolver",
                "complete",
                String.format(
                        "category=%s, mode=%s, confidence=%d",
                        resolution.getCategory(),
                        resolution.getResolverMode(),
                        resolution.getConfidence()
                )
        );

        CategoryResult result = categoryHandlerRegistry.get(resolution.getCategory()).handle(state, emitter);
        state.setFinalResponse(result.getFinalResponse());

        return result;
    }
}