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

/**
 * CategoryChatOrchestrator는 여러 단계의 처리를 조율하는 오케스트레이터이다.
 * <p>주요 기능: category chat orchestrator 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CategoryChatOrchestrator {

    /** categoryResolver 값을 보관한다. */
    private final CategoryResolver categoryResolver;
    /** categoryHandlerRegistry 값을 보관한다. */
    private final CategoryHandlerRegistry categoryHandlerRegistry;
    /** conversationMemoryService 값을 보관한다. */
    private final ConversationMemoryService conversationMemoryService;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @param conversationId 대화 식별자
     * @param requestedCategory requestedCategory 값
     * @param emitter SSE 이벤트 전송 객체
     * @return CategoryResult 타입의 처리 결과
     */
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