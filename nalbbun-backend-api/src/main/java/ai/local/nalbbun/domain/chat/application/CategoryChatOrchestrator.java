package ai.local.nalbbun.domain.chat.application;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.domain.category.CategoryResolver;
import ai.local.nalbbun.domain.category.model.CategoryResolution;
import ai.local.nalbbun.domain.category.model.CategoryResult;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.category.model.ExecutionMode;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import ai.local.nalbbun.common.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;

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
                                  ExecutionMode requestedExecutionMode,
                                  String promptId,
                                  SseEmitter emitter) {

        ConversationState state = new ConversationState();
        state.setConversationId(conversationId);
        state.setUserQuery(userQuery);
        state.setRequestedCategory(requestedCategory);
        state.setRequestedExecutionMode(requestedExecutionMode);
        state.setPromptId(promptId);

        CategoryResolution resolution = categoryResolver.resolve(userQuery, requestedCategory, requestedExecutionMode);
        state.setResolvedCategory(resolution.getCategory());
        state.setResolvedExecutionMode(resolution.getExecutionMode());
        state.getAttributes().put("resolverMode", resolution.getResolverMode());
        state.getAttributes().put("resolverReason", resolution.getReason());
        state.getAttributes().put("resolverConfidence", resolution.getConfidence());
        state.getAttributes().put("executionMode", resolution.getExecutionMode().name());

        conversationMemoryService.addUserMessage(conversationId, resolution.getCategory(), userQuery);

        agentEventPublisher.sendDetails(
                emitter,
                "CategoryResolver",
                "complete",
                String.format(
                        "category=%s, mode=%s, confidence=%d, executionMode=%s",
                        resolution.getCategory(),
                        resolution.getResolverMode(),
                        resolution.getConfidence(),
                        resolution.getExecutionMode()
                ),
                Map.of(
                        "category", resolution.getCategory().name(),
                        "resolverMode", resolution.getResolverMode(),
                        "confidence", resolution.getConfidence(),
                        "executionMode", resolution.getExecutionMode().name(),
                        "reason", resolution.getReason()
                )
        );

        CategoryResult result = categoryHandlerRegistry.get(resolution.getCategory()).handle(state, emitter);
        state.setFinalResponse(result.getFinalResponse());

        return result;
    }
}
